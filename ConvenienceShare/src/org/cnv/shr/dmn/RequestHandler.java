package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;

public class RequestHandler extends Thread
{	
	private boolean quit;
	ServerSocket socket;
	int port;
	
	public RequestHandler(ServerSocket socket)
	{
		this.port = socket.getLocalPort();
		LogWrapper.getLogger().info("Starting on port " + port);
		this.socket = socket;
	}
	
	public void quit()
	{
		quit = true;
		try
		{
			socket.close();
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close socket.", ex);
		}
	}
	
	@Override
	public void run()
	{
		while (!quit)
		{
			try
			{
				if (socket.isClosed())
				{
					socket = new ServerSocket(port);
				}
				socket.setReuseAddress(true);
				// socket.setSoTimeout(5000);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect on " + port + ": " + e.getMessage(), e);
				continue;
			}

			try (Socket accept = socket.accept();)
			{
				Services.networkManager.handleConnection(accept);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect on " + port + ": " + e.getMessage(), e);
				continue;
			}
			catch (Exception t)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to handle connection on " + port, t);
			}
		}
		LogWrapper.getLogger().info("Quitting on port " + port);
	}
}
