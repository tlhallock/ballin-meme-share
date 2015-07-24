package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.WaitForObject;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class HandshakeServer extends HandShake
{
	private static Executor handlerThreads = Executors.newCachedThreadPool();
	
	
	private PortStatus[] ports;
	private boolean quit;
	
	public HandshakeServer(int begin, int end)
	{
		ports = new PortStatus[end - begin - 1];
		for (int i = 0; i < end; i++)
		{
			ports[i] = new PortStatus(begin + i + 1);
		}
	}
	
	public void handleConnections(int port, int beginBegin, int portEnd)
	{
		try (ServerSocket serverSocket = new ServerSocket();)
		{
			while (!quit)
			{
				try (Socket socket = serverSocket.accept(); JsonParser parser = TrackObjectUtils.createParser(new SnappyFramedInputStream(socket.getInputStream(), true)); JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(socket.getOutputStream()));)
				{
					generator.writeStartArray();

					RSAPublicKey localKey = Services.keyManager.getPublicKey();
					sendInfo(generator, localKey);

					RSAPublicKey preferredKey = readInfo();
					if (preferredKey == null)
						return;
					if (!authenticateTheClient())
						return;
					if (!authenticateToClient())
						return;

					PortStatus chosenPort = createHandlerRunnable().get();

					generator.writeStartObject();
					if (chosenPort != null)
					{
						generator.write("port", chosenPort.port);
					}
					else
					{
						generator.write("port", -1);
					}
					generator.writeEnd();

					generator.writeEnd();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private WaitForObject<PortStatus> createHandlerRunnable()
	{
		WaitForObject<PortStatus> wait = new WaitForObject<>(10 * 1000);
		
		handlerThreads.execute(() -> {
			PortStatus reservedPort = reserveAPort();
			try
			{
				wait.set(reservedPort);
				
				try (ServerSocket serverSocket = new ServerSocket();
						)
				{
					
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
}
