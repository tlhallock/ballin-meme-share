package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.cnv.shr.msg.Message;

public class RequestHandler extends Thread
{	
	public boolean quit;
	
	public void quit()
	{
		quit = true;
		interrupt();
	}
	
	public void run()
	{
		while (!quit)
		{
			try (ServerSocket socket = new ServerSocket(Services.settings.defaultPort);)
			{
				handleConnection(socket.accept());
			}
			catch (Exception ex)
			{
				ex.printStackTrace(Services.logger.logStream);
			}
		}
	}
	
	private void handleConnection(Socket accept) throws IOException
	{
		try (InputStream inputStream = accept.getInputStream();)
		{
			Message request = Message.readMsg(inputStream);
			if (!request.authenticate())
			{
				return;
			}

			perform(request);
		}
	}
	
	private void perform(final Message request)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					request.perform();
				}
				catch (Exception e)
				{
					e.printStackTrace(Services.logger.logStream);
				}
			}
		});
	}
}
