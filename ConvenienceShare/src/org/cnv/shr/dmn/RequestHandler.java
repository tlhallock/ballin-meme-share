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
		Services.logger.println("Starting on port " + port);
		this.socket = new ServerSocket(port);
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
			Services.logger.print(ex);
		}
	}
	
	@Override
	public void run()
	{
		while (!quit)
		{

			Socket accept;
			try
			{
				if (socket.isClosed())
				{
					socket = new ServerSocket(port);
				}
				socket.setReuseAddress(true);
				// socket.setSoTimeout(5000);
				accept = socket.accept();
			}
			catch (IOException e)
			{
				Services.logger.println("Unable to connect on " + port + ": " + e.getMessage());
				Services.logger.print(e);
				continue;
			}
			try
			{
				Services.networkManager.handleConnection(accept);
			}
			catch (Exception t)
			{
				Services.logger.println(t);
			}
		}
		Services.logger.println("Quitting on port " + port);
	}
}
