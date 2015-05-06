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

	public DbConnectionCache() throws SQLException, IOException, ClassNotFoundException
	{
		Misc.ensureDirectory(Services.settings.dbFile.get(), true);
		Class.forName("org.h2.Driver");
		
		Connection c = getConnection();
		Services.logger.logStream.println("Creating database.");
		DbTables.createDb(c);
	}

	public Connection getConnection()
	{
		long id = Thread.currentThread().getId();
		id = 0;
		Connection returnValue = connections.get(id);
		if (returnValue == null)
		{
			try
			{
				returnValue = DriverManager.getConnection("jdbc:h2:./test_" + Services.settings.servePortBegin.get() + ".db", "sa", "");
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			connections.put(id, returnValue);
		}
		return returnValue;
	}
}
