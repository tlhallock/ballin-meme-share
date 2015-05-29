package org.cnv.shr.cnctn;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.LogWrapper;

public class ConnectionRunnable implements Runnable
{
	Communication connection;
	Authenticator authentication;
	ConnectionStatistics stats;
	
	public ConnectionRunnable(final Communication c, final Authenticator authentication)
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
				catch (final Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Error performing message task:" + request.getClass().getName(), e);
					LogWrapper.getLogger().info("Closing connection.");
					break;
				}
			}
			Services.notifications.connectionClosed(connection);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Error creating message:", e);
			Services.quiter.quit();
		}
		catch (final Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Error with connection:", ex);
		}
		finally
		{
			cleanup();
		}
	}

	private void cleanup()
	{
		try
		{
			ensureClosed();
		}
		finally
		{
			try
			{
				Services.notifications.connectionClosed(connection);
			}
			finally
			{
				Services.networkManager.remove(this);
			}
		}
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
		catch (final InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
			try
			{
				connection.getSocket().close();
			}
			catch (final IOException e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Can't close at all", e1);
			}
		}
		catch (final IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Can't close" , e);
		}
	}
	
	void die()
	{
		try
		{
			connection.getSocket().close();
		}
		catch (final IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Can't die", e);
		}
	}
}
