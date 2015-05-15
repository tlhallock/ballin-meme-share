package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RequestHandler implements Runnable
{	
	private boolean quit;
	ServerSocket socket;
	
	public RequestHandler(ServerSocket socket)
	{
		this.socket = socket;
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
		try
		{
			socket.setReuseAddress(true);
			while (!quit)
			{
				// socket.setSoTimeout(5000);
				Socket accept = socket.accept();
				if (quit) return;
				Services.networkManager.handleConnection(accept);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Services.logger.println("Quitting:");
		}
	}
}
