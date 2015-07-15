
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.db.h2;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Arguments;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DbConnectionCache extends TimerTask
{
	private HashMap<Long, ConnectionWrapper> connections = new HashMap<>();

	public DbConnectionCache(Arguments args) throws SQLException, IOException, ClassNotFoundException
	{
		Class.forName("org.h2.Driver");
		
		try (ConnectionWrapper c = getThreadConnection();)
		{
			if (c == null)
			{
				throw new SQLException("Unable to create connection!");
			}
			if (args.deleteDb)
			{
				LogWrapper.getLogger().info("Deleting database.");
				DbTables.deleteDb(c);
			}
			else
			{
				LogWrapper.getLogger().info("Creating database.");
				DbTables.createDb(c);
			}
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
			Misc.timer.scheduleAtFixedRate(wrapper, 1000, 1000);
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
	
	public void debug()
	{
		try
		{
			LogWrapper.getLogger().info(debug(new StringBuilder()).toString());
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to debug.", e);
		}
	}
	
	public synchronized StringBuilder debug(StringBuilder builder) throws SQLException
	{
		for (Entry<Long, ConnectionWrapper> entry : connections.entrySet())
		{
			builder.append("Thread " + entry.getKey());
			entry.getValue().debug(builder);
		}
		return builder;
	}
	

	public void setAutoCommit(boolean value)
	{
		try (ConnectionWrapper conn = Services.h2DbCache.getThreadConnection())
		{
			conn.setAutoCommit(value);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to set autocommit to " + value, e);
		}
	}
}
