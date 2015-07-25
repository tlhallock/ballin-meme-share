package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.WaitForObject;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

import de.flexiprovider.core.rijndael.RijndaelKey;
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
					 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(socket.getOutputStream()));
					 JsonParser parser = TrackObjectUtils.createParser(         new SnappyFramedInputStream(socket.getInputStream(), true));)
			{
				generator.writeStartArray();

				RSAPublicKey localKey = Services.keyManager.getPublicKey();
				sendInfo(generator, localKey, null);

				RemoteInfo preferredKey = readInfo(parser);
				if (preferredKey == null)
					return;
				if (!authenticateTheRemote(generator, parser, preferredKey.ident, false))
					return;
				if (!authenticateToRemote(generator, parser))
					return;

				DbMachines.updateMachineInfo(preferredKey.ident, preferredKey.name, preferredKey.publicKey, socket.getInetAddress().getHostAddress(), preferredKey.port);

				RijndaelKey outgoing = KeysService.createAesKey();
				EncryptionKey.sendOpenParams(generator, preferredKey.publicKey, outgoing);
				RijndaelKey incoming = new EncryptionKey(parser, localKey).encryptionKey;

				PortStatus chosenPort = createHandlerRunnable(outgoing, incoming, preferredKey.ident, preferredKey.reason).get();
				if (chosenPort == null)
				{
					// send wait message
					return;
				}
				generator.writeStartObject();
				generator.write("port", chosenPort.port);
				generator.writeEnd();
				generator.flush();

				generator.writeEnd();
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, null, e);
			}
		}
	}

	private WaitForObject<PortStatus> createHandlerRunnable(
			RijndaelKey outgoing,
			RijndaelKey incoming,
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
								 reason);)
				{
					new ConnectionRunnable(communication).run();
				}
				catch (InvalidKeyException | IOException ex)
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
	}





	public void stop()
	{
		// TODO Auto-generated method stub
		
	}
}
