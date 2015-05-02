package org.cnv.shr.dmn;

import java.net.BindException;
import java.net.ServerSocket;

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
			try (ServerSocket socket = new ServerSocket(Services.settings.defaultPort.get());)
			{
				Services.networkManager.handleConnection(socket.accept());
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
}
