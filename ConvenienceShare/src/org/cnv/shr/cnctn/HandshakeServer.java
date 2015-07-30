package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AcceptKey;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.SocketStreams;
import org.cnv.shr.util.WaitForObject;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class HandshakeServer extends HandShake implements Runnable
{
	private static Executor handlerThreads = Executors.newCachedThreadPool();
	
	ServerSocket serverSocket;
	private int port;
	private PortStatus[] ports;
	private boolean quit;
	
	public HandshakeServer(ServerSocket serverSocket, int begin, int end) throws IOException
	{
		ports = new PortStatus[end - begin];
		for (int i = 0; i < ports.length; i++)
		{
			ports[i] = new PortStatus(begin + i);
		}
		this.serverSocket = serverSocket;
	}
	
	public void run()
	{
		handleConnections();
	}
	
	public void handleConnections()
	{
		while (!quit)
		{
			try (Socket socket = serverSocket.accept(); 
					 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(SocketStreams.newSocketOutputStream(socket)), true);
					 JsonParser parser = TrackObjectUtils.createParser(         new SnappyFramedInputStream( SocketStreams.newSocketInputStream (socket), true), true);)
			{
				String hostAddress = socket.getInetAddress().getHostAddress();
				LogWrapper.getLogger().info("Accepted connection from " + hostAddress);
				
				generator.writeStartArray();

				RSAPublicKey localKey = Services.keyManager.getPublicKey();
				sendInfo(generator, localKey, null);

				expect(parser, JsonParser.Event.START_ARRAY);
				RemoteInfo remoteInfo = readInfo(parser);
				
				if (remoteInfo.showGui && Misc.collectIps().contains(hostAddress))
				{
					UserActions.showGui();
					return;
				}
				
				if (remoteInfo == null)
					return;

				if (!verifyMachine(remoteInfo.ident, hostAddress, remoteInfo.port, remoteInfo.name, remoteInfo.publicKey))
				{
					return;
				}
				
				if (!authenticateTheRemote(generator, parser, remoteInfo.ident, false)
						&& !AcceptKey.showAcceptDialogAndWait(hostAddress, remoteInfo.name, remoteInfo.ident, remoteInfo.publicKey, 10))
				{
					UserMessage.addChangeKeyMessage(hostAddress, remoteInfo.ident, remoteInfo.publicKey);
					return;
				}
				if (!authenticateToRemote(generator, parser))
					return;

				DbMachines.updateMachineInfo(
						remoteInfo.ident, 
						remoteInfo.name, 
						remoteInfo.publicKey, 
						hostAddress, 
						remoteInfo.port);

				KeyInfo outgoing = new KeyInfo();
				outgoing.generate(generator, remoteInfo.publicKey);
				
				KeyInfo incoming = new KeyInfo(parser, localKey);

				PortStatus chosenPort = createHandlerRunnable(outgoing, incoming, remoteInfo.ident, remoteInfo.reason).get();
				
				writePort(generator, chosenPort);

				generator.writeEnd();
				generator.close();
				expect(parser, JsonParser.Event.END_ARRAY);

				LogWrapper.getLogger().info("Handshake complete.");
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Error in handshake server", e);
			}
		}
	}

	private static void writePort(JsonGenerator generator, PortStatus chosenPort)
	{
		generator.writeStartObject();
		if (chosenPort == null)
		{
			generator.write("port", "None");
			LogWrapper.getLogger().info("All ports are busy.");
		}
		else
		{
			generator.write("port", chosenPort.port);
			LogWrapper.getLogger().info("Allocated port " + chosenPort.port);
		}
		generator.writeEnd();
		generator.flush();
	}

	private WaitForObject<PortStatus> createHandlerRunnable(
			KeyInfo outgoing,
			KeyInfo incoming,
			String remoteIdentifer,
			String reason)
	{
		WaitForObject<PortStatus> wait = new WaitForObject<>(10 * 1000);
		
		handlerThreads.execute(() -> {
			PortStatus reservedPort = reserveAPort();
			if (reservedPort == null)
			{
				return;
			}
			try
			{
				try (Socket socket = getClientSocket(reservedPort, wait);
						 Communication communication = new Communication(
								 socket,
								 incoming,
								 outgoing,
								 remoteIdentifer,
								 reason,
								 true);)
				{
					new ConnectionRunnable(communication).run();
				}
				catch (InvalidAlgorithmParameterException | InvalidKeyException | IOException ex)
				{
					LogWrapper.getLogger().log(Level.INFO, "Error while trying to connect to client.", ex);
				}
			}
			finally
			{
				reservedPort.free();
			}
		});
		return wait;
	}
	
	private PortStatus reserveAPort()
	{
		synchronized (ports)
		{
			for (PortStatus status : ports)
			{
				if (status.inUse)
				{
					continue;
				}
				status.inUse = true;
				return status;
			}
		}
		return null;
	}

	private static Socket getClientSocket(PortStatus reservedPort, WaitForObject<PortStatus> wait) throws IOException
	{
		try (ServerSocket socket = new ServerSocket(reservedPort.port);)
		{
			wait.set(reservedPort);
			return socket.accept();
		}
	}

	public void stop()
	{
		quit = true;
		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void debug()
	{
		synchronized (ports)
		{
			LogWrapper.getLogger().info("Port status:");
			for (PortStatus status : ports)
			{
				LogWrapper.getLogger().info(status.toString());
			}
		}
	}
	
	private static final class PortStatus
	{
		int port;
		boolean inUse;
		
		public PortStatus(int port)
		{
			this.port = port;
		}

		public void free()
		{
			inUse = false;
		}
		
		public String toString()
		{
			return "\t" + port + ": " + (inUse ? " in use" : "free"); 
		}
	}
}
