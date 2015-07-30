package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.swing.JOptionPane;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.AlternativeAddresses;
import org.cnv.shr.gui.AcceptKey;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.SocketStreams;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

public class HandshakeClient extends HandShake
{
	private static Socket connect(ConnectionParams params)
	{
		try
		{
			return new Socket(params.ip, params.port);
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().info("Unable to connect to " + params.ip + " on port " + params.port + ", trying others if available. " + ex.getMessage());
		}
		if (params.origin == null || params.identifier == null)
		{
			return null;
		}
		if (Services.trackers.getClients().isEmpty())
		{
			JOptionPane.showMessageDialog(params.origin, "ConvenienceShare was unable to connect to "
					+ params.identifier + " at " + params.ip + ":" + params.port + ".\n",
					"Unable to connect",
					JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(params.origin, "ConvenienceShare was unable to connect to "
				+ params.identifier + " at " + params.ip + ":" + params.port + ".\n"
				+ "Would you like to connect to a tracker to see if the ip address has changed?",
				"Unable to connect",
				JOptionPane.YES_NO_OPTION))
		{
			return null;
		}
		
		AlternativeAddresses findAlternativeUrls = Services.trackers.findAlternativeUrls(params.identifier);
		findAlternativeUrls.remove(params.ip, params.port);
		if (findAlternativeUrls.isEmpty())
		{
			JOptionPane.showConfirmDialog(
					params.origin, 
					"No other addresses found.",
					"Unable to connect.",
					JOptionPane.WARNING_MESSAGE);
				return null;
		}
		
		for (String alternative : findAlternativeUrls.getIps())
		{
			if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
					params.origin, 
					"A tracker listed another url listed for " + params.identifier + " at " + findAlternativeUrls.describe(alternative) + ".\n"
						+ "Would you like to try this one?",
					"Found another address",
					JOptionPane.YES_NO_OPTION))
			{
				continue;
			}
			
			for (Integer port : findAlternativeUrls.getPorts(alternative))
			{
				try
				{
					return new Socket(alternative, port);
				}
				catch (IOException ex)
				{
					LogWrapper.getLogger().info("Unable to connect to " + alternative + " on port " + port + ", trying others if available. " + ex.getMessage());
				}
			}
		}
		return null;
	}
	
	private static Socket connectWithException(ConnectionParams params) throws IOException
	{
		Socket connect = connect(params);
		if (connect != null) return connect;
		throw new IOException("Not able to connect.");
	}

	public static HandShakeResults initiateHandShake(ConnectionParams params)
	{
		try (Socket socket           = connectWithException(params);
				 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(SocketStreams.newSocketOutputStream(socket)), true);
				 JsonParser parser       = TrackObjectUtils.createParser(   new SnappyFramedInputStream( SocketStreams.newSocketInputStream (socket), true), true);)
		{
			generator.writeStartArray();

			LogWrapper.getLogger().info("Beginning handshake.");

			HandShakeResults results = new HandShakeResults();
			results.localKey = Services.keyManager.getPublicKey();
			
			sendInfo(generator, results.localKey, params.reason);

			expect(parser, JsonParser.Event.START_ARRAY);
			RemoteInfo remoteInfo = readInfo(parser);
			if (remoteInfo == null)
				return null;
			
			if (!verifyMachine(remoteInfo.ident, socket.getInetAddress().getHostAddress(), remoteInfo.port, remoteInfo.name, remoteInfo.publicKey))
			{
				return null;
			}
		
			results.remoteKey = remoteInfo.publicKey;
			results.ident = remoteInfo.ident;
			if (!authenticateToRemote(generator, parser))
			{
				return null;
			}
			
			if (!authenticateTheRemote(generator, parser, params.identifier != null ? params.identifier : remoteInfo.ident, params.acceptAllKeys)
					&& !AcceptKey.showAcceptDialogAndWait(params.ip, remoteInfo.name, remoteInfo.ident, remoteInfo.publicKey, 10))
			{
				UserMessage.addChangeKeyMessage(params.ip, remoteInfo.ident, remoteInfo.publicKey);
				return null;
			}

			DbMachines.updateMachineInfo(
					remoteInfo.ident,
					remoteInfo.name,
					remoteInfo.publicKey,
					socket.getInetAddress().getHostAddress(),
					params.port);
			

			LogWrapper.getLogger().info("Creating aes key for this connection.");
			results.outgoing = new KeyInfo();
			results.outgoing.generate(generator, remoteInfo.publicKey);
			generator.writeEnd();

			results.incoming = new KeyInfo(parser, results.localKey);
			
			results.port = readPort(parser);

			generator.close();
			expect(parser, JsonParser.Event.END_ARRAY);
			
			LogWrapper.getLogger().info("Handshake complete.");

			return results;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Error in handshake", e);
			return null;
		}
	}

	private static int readPort(JsonParser parser)
	{
		int port = -1;
		
		expect(parser, JsonParser.Event.START_OBJECT);
		
		outer:
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				if (!parser.getString().equals("port"))
					throw new RuntimeException("Expected to see confirmation that we are authenticated.");
				break;
			case VALUE_STRING:
				if (parser.getString().equals("None"))
				{
					LogWrapper.getLogger().info("Remote has no free ports...");
				}
				break;
			case VALUE_NUMBER:
				port = parser.getInt();
				LogWrapper.getLogger().info("Remote has given us port " + port);
				break;
			case END_OBJECT:
				break outer;
			default:
				LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr("port", parser, e, null));
			}
		}
		return port;
	}
}
