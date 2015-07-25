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
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
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
				 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(socket.getOutputStream()));
				 JsonParser parser       = TrackObjectUtils.createParser(   new SnappyFramedInputStream(socket.getInputStream(), true));)
		{
			generator.writeStartArray();

			HandShakeResults results = new HandShakeResults();
			results.localKey = Services.keyManager.getPublicKey();
			
			sendInfo(generator, results.localKey, params.reason);
			RemoteInfo remoteKey = readInfo(parser);
			results.remoteKey = remoteKey.publicKey;
			
			if (!authenticateToRemote(generator, parser))
			{
				return null;
			}
			
			if (!authenticateTheRemote(generator, parser, params.identifier != null ? params.identifier : remoteKey.ident, params.acceptAllKeys))
			{
				return null;
			}

			DbMachines.updateMachineInfo(
					remoteKey.ident,
					remoteKey.name,
					remoteKey.publicKey,
					socket.getInetAddress().getHostAddress(),
					params.port);
			
			results.outgoing = KeysService.createAesKey();
			EncryptionKey.sendOpenParams(generator, remoteKey.publicKey, results.outgoing);

			EncryptionKey connectionOpenedParams = new EncryptionKey(parser, results.localKey);
			results.incoming = connectionOpenedParams.encryptionKey;
			
			results.port = readPort(parser);
			
			generator.writeEnd();
			return results;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, null, e);
			return null;
		}
	}

	private static int readPort(JsonParser parser)
	{
		int port = -1;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				if (!parser.getString().equals("port"))
					throw new RuntimeException("Expected to see confirmation that we are authenticated.");
				break;
			case VALUE_NUMBER:
				port = parser.getInt();
				break;
			case END_OBJECT:
				break;
			default:
				LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr("port", parser, e));
			}
		}
		return port;
	}
}
