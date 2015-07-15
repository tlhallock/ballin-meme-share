
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;

import org.cnv.shr.util.Misc;

public class ConnectionWrapper3
{
	private static final int NUM_STATEMENTS = 100;
	private StatementWrapper[] statements = new StatementWrapper[NUM_STATEMENTS];
	
	private Connection connection;
	
	private int inactiveCount;
	private boolean active;
	
	// for nearly-lock free
	private volatile long modCount;
	
	public ConnectionWrapper3(Connection connection)
	{
		this.connection = connection;
	}

	public void run()
	{
		if (active)
		{
			inactiveCount = 0;
			return;
		}
		
		if (inactiveCount++ > 5)
		{
			// close
		}
	}


	public synchronized void closeConnections() throws SQLException
	{
		modCount++;
		for (int i = 0; i < statements.length; i++)
		{
			StatementWrapper wrapper = statements[i];
			statements[i] = null;
			modCount++;
			wrapper.shutDown();
		}
		connection = null;
	}
	
	private StatementWrapper getStatement(QueryWrapper wrapper, Integer k) throws SQLException
	{
		long pModCount = modCount;
		StatementWrapper statement = statements[wrapper.index];
		if (statement != null) 
		{
			statement.setInUse(true);
			if (pModCount == modCount)
			{
				return statement;
			}
			else
			{
				statement.setInUse(false);
				return null;
			}
		}
		
		synchronized (this)
		{
			if (k == null)
			{
				statements[wrapper.index] = statement = new StatementWrapper(
						connection.prepareStatement(wrapper.query));
			}
			else
			{
				statements[wrapper.index] = statement = new StatementWrapper(
						connection.prepareStatement(wrapper.query, k));
			}
			statement.inUse = true;
			return statement;
		}
	}

	public StatementWrapper prepareStatement(QueryWrapper wrapper) throws SQLException
	{
		active = true;
		StatementWrapper statement = getStatement(wrapper, null);
		if (statement == null)
		{
			synchronized (this)
			{
				statement = getStatement(wrapper, null);
			}
		}
		return statement;
	}


	public StatementWrapper prepareStatement(QueryWrapper wrapper, int returnGeneratedKeys) throws SQLException
	{
		active = true;
		StatementWrapper statement = getStatement(wrapper, returnGeneratedKeys);
		if (statement == null)
		{
			synchronized (this)
			{
				statement = getStatement(wrapper, returnGeneratedKeys);
			}
		}
		return statement;
	}
	
	public PreparedStatement prepareNewStatement(String string) throws SQLException
	{
		active = true;
		return connection.prepareStatement(string);
	}

	public static final class QueryWrapper
	{
		private int index;
		private String query;
		
		public QueryWrapper(String str)
		{
			index = getNextQueryWrapperId();
			query = str;
		}

		static int nextId = 0;
		private static synchronized int getNextQueryWrapperId()
		{
			return nextId++;
		}
	}
	
	/**
	 * I don't like the class, but it was easier than removing all the try () {...} blocks.
	 *
	 */
	public static final class StatementWrapper extends TimerTask implements AutoCloseable
	{
		private PreparedStatement statement;
		private boolean inUse;
		
		public StatementWrapper(PreparedStatement statement)
		{
			this.statement = statement;
		}

		public void setInUse(boolean b)
		{
			inUse = b;
		}

		public void shutDown() throws SQLException
		{
			Misc.timer.scheduleAtFixedRate(this, 1000, 1000);
		}

		@Override
		public void close()
		{
			setInUse(false);
		}

		public void setInt(int i, Integer id) throws SQLException
		{
			statement.setInt(i,  id);
		}

		public ResultSet executeQuery() throws SQLException
		{
			return statement.executeQuery();
		}

		public void execute() throws SQLException
		{
			statement.execute();
		}

		public void setBoolean(int i, boolean done) throws SQLException
		{
			statement.setBoolean(i, done);
		}

		public void setLong(int i, long begin) throws SQLException
		{
			statement.setLong(i, begin);
		}

		public void setString(int i, String checksum) throws SQLException
		{
			statement.setString(i, checksum);
		}

		public void executeUpdate() throws SQLException
		{
			statement.executeUpdate();
		}

		public ResultSet getGeneratedKeys() throws SQLException
		{
			return statement.getGeneratedKeys();
		}

		@Override
		public void run()
		{
			if (inUse)
			{
				return;
			}
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			finally
			{
				cancel();
			}
		}
	}
}
