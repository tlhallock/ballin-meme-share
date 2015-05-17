package org.cnv.shr.cnctn;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;

public class ConnectionRunnable implements Runnable
{
	Communication connection;
	Authenticator authentication;
	ConnectionStatistics stats;
	
	public ConnectionRunnable(Communication c, Authenticator authentication)
	{
		this.connection = c;
		this.authentication = authentication;
	}
	
	@Override
	public void run()
	{
		try
		{
			while (connection.needsMore())
			{
				Message request = Services.msgReader.readMsg(connection.getReader());
				if (request == null || !authentication.authenticate(request))
				{
					break;
				}

				try
				{
					request.perform(connection);
				}
				catch (Exception e)
				{
					Services.logger.println("Error performing message task:");
					Services.logger.print(e);
				}
			}
			Services.notifications.connectionClosed(connection);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			Services.logger.println("Error creating message:");
			Services.logger.print(e);
			Services.quiter.quit();
		}
		catch (Exception ex)
		{
			Services.logger.println("Error with connection:");
			Services.logger.print(ex);
		}
		finally
		{
			ensureClosed();
		}
		Services.networkManager.remove(this);
	}

	private void ensureClosed()
	{
		try
		{
			int ndx = 0;
			while (!connection.isClosed())
			{
				if (ndx++ < 10)
				{
					Thread.sleep(200);
					continue;
				} 
				connection.getSocket().close();
				System.out.println("Closed");
			}
		}
		catch (InterruptedException e)
		{
			Services.logger.print(e);
			try
			{
				connection.getSocket().close();
			}
			catch (IOException e1)
			{
				Services.logger.print(e1);
			}
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}
	
	void die()
	{
		try
		{
			connection.getSocket().close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
