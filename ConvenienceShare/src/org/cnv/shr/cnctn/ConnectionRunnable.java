package org.cnv.shr.cnctn;

import java.io.InputStream;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.DoneResponse;
import org.cnv.shr.msg.Message;

public class ConnectionRunnable implements Runnable
{
	Communication connection;
	AuthenticationWaiter authentication;
	ConnectionStatistics stats;
	
	public ConnectionRunnable(Communication c, AuthenticationWaiter authentication)
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
				Message request = Services.msgReader.readMsg(stats, connection.getIn());
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
					Services.logger.logStream.println("Error performing message task:");
					e.printStackTrace(Services.logger.logStream);
				}
			}

			connection.getSocket().shutdownOutput();
		}
		catch (Exception ex)
		{
			Services.logger.logStream.println("Error with connection:");
			ex.printStackTrace(Services.logger.logStream);
		}
		Services.notifications.connectionClosed(connection);
	}
}
