package org.cnv.shr.db.h2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TimerTask;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;

public class DbConnectionCache extends TimerTask
{
	private HashMap<Long, ConnectionWrapper> connections = new HashMap<>();

	public DbConnectionCache(boolean fresh) throws SQLException, IOException, ClassNotFoundException
	{
		Class.forName("org.h2.Driver");
		
		try (ConnectionWrapper c = getThreadConnection();)
		{
			if (fresh)
			{
				Services.logger.println("Deleting database.");
				DbTables.deleteDb(c);
			}
			Services.logger.println("Creating database.");
			DbTables.createDb(c);
		}
	}

	Connection c;
	public synchronized ConnectionWrapper getThreadConnection()
	{
		long id = Thread.currentThread().getId();
		ConnectionWrapper returnValue = connections.get(id);
		try
		{
			if (returnValue == null)
			{
				Misc.ensureDirectory(Services.settings.dbFile.get(), true);
				String file = Services.settings.dbFile.get().getAbsolutePath();
				Services.logger.println("DbFile: " + file);
				
				if (c == null || c.isClosed())
				{
					c = DriverManager.getConnection("jdbc:h2:" + file, "sa", "");
				}
				
				returnValue = new ConnectionWrapper(c);

				Services.logger.println("Connect with:");
				Services.logger.println("java -cp ConvenienceShare/libs/h2-1.4.187.jar org.h2.tools.Shell ".trim());
				Services.logger.println("jdbc:h2:" + file);
				Services.logger.println("org.h2.Driver                                                    ".trim());
				Services.logger.println("sa                                                               ".trim());
				Services.logger.println("                                                                 ".trim());
				Services.logger.println("                                                                 ".trim());

				connections.put(id, returnValue);
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		// slight sync issue here...
		returnValue.setInUse();
		return returnValue;
	}
	
	public synchronized void flush()
	{
		for (final ConnectionWrapper wrapper : connections.values())
		{
			Services.timer.scheduleAtFixedRate(wrapper, 1000, 1000);
//			Services.timer.schedule(new TimerTask() {
//				@Override
//				public void run()
//				{
//					wrapper.shutdown();
//				}}, 10 * 60 * 1000);
		}
		connections = new HashMap<>();
	}
	
	public synchronized void close()
	{
		for (ConnectionWrapper wrapper : connections.values())
		{
			wrapper.shutdown();
		}
		connections.clear();
	}

	@Override
	public void run()
	{
		flush();
	}
}
