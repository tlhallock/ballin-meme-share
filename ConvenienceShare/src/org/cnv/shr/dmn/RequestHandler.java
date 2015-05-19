package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RequestHandler extends Thread
{	
	private boolean quit;
	ServerSocket socket;
	int port;
	
	public RequestHandler(int port) throws IOException
	{
		this.port = port;
		this.socket = new ServerSocket(port);
	}
	
	public void quit()
	{
		quit = true;
		// kick the socket
		try (Socket s = new Socket(socket.getInetAddress(), socket.getLocalPort()))
		{
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		try
		{
			socket.close();
		}
		catch (IOException ex)
		{
			Services.logger.print(ex);
		}
	}
	
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
				Socket accept = socket.accept();
				Services.networkManager.handleConnection(accept);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				Services.logger.println("Quitting:");
			}
		}
	}
}
