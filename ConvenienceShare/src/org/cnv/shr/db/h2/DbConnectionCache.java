package org.cnv.shr.db.h2;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;
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
				LogWrapper.getLogger().info("Deleting database.");
				DbTables.deleteDb(c);
			}
			LogWrapper.getLogger().info("Creating database.");
			DbTables.createDb(c);
		}

		String file = getDbFile();
		LogWrapper.getLogger().info("DbFile: " + file );
		LogWrapper.getLogger().info("Connect with:");
		LogWrapper.getLogger().info("java -cp ConvenienceShare/libs/h2-1.4.187.jar org.h2.tools.Shell ".trim());
		LogWrapper.getLogger().info("jdbc:h2:" + file);
		LogWrapper.getLogger().info("org.h2.Driver                                                    ".trim());
		LogWrapper.getLogger().info("sa                                                               ".trim());
		LogWrapper.getLogger().info("                                                                 ".trim());
		LogWrapper.getLogger().info("                                                                 ".trim());
	}

	public synchronized ConnectionWrapper getThreadConnection()
	{
		long id = Thread.currentThread().getId();
		ConnectionWrapper returnValue = connections.get(id);
		try
		{
			if (returnValue == null)
			{
				if (returnValue == null || returnValue.isClosed())
				{
					returnValue = new ConnectionWrapper(DriverManager.getConnection("jdbc:h2:" + getDbFile(), "sa", ""));
				}
				
				connections.put(id, returnValue);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to create db connection", e);
		}
		returnValue.setInUse();
		return returnValue;
	}

	private String getDbFile()
	{
		Misc.ensureDirectory(Services.settings.dbFile.get(), true);
		return Services.settings.dbFile.get().getAbsolutePath();
	}
	
	public synchronized void flush()
	{
		LogWrapper.getLogger().info("Flushing db connections.");
		
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
