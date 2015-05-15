package org.cnv.shr.db.h2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Hashtable;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;

public class DbConnectionCache
{
	private Hashtable<Long, Connection> connections = new Hashtable<>();

	public DbConnectionCache(boolean fresh) throws SQLException, IOException, ClassNotFoundException
	{
		Misc.ensureDirectory(Services.settings.dbFile.get(), true);
		Class.forName("org.h2.Driver");
		
		Connection c = getConnection();
		if (fresh)
		{
			Services.logger.println("Deleting database.");
			DbTables.deleteDb(c);
		}
		Services.logger.println("Creating database.");
		DbTables.createDb(c);
	}

	public Connection getConnection()
	{
		long id = Thread.currentThread().getId();
		Connection returnValue = connections.get(id);
		if (returnValue == null)
		{
			try
			{
				String file = Services.settings.dbFile.get().getAbsolutePath();
				Services.logger.println("DbFile: " + file);
				returnValue = DriverManager.getConnection("jdbc:h2:" + file, "sa", "");
			} catch (SQLException e)
			{
				Services.logger.print(e);
			}
			connections.put(id, returnValue);
		}
		return returnValue;
	}
	
	public void close()
	{
		try
		{
			getConnection().close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}
