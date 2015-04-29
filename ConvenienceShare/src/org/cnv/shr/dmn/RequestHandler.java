package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.MessageReader;

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
			catch(BindException ex)
			{
				Services.logger.logStream.println("Port already in use.");
				Services.logger.logStream.println("Quiting.");
				Main.quit();
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
			Message request = Services.msgReader.readMsg(accept.getInetAddress(), inputStream);
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
