
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
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;

public class ConnectionWrapper extends TimerTask implements AutoCloseable
{
	private static final int NUM_STATEMENTS = 100;
	private StatementWrapper[] statements = new StatementWrapper[NUM_STATEMENTS];
	
	private Connection connection;
	private int inUse;
	
	public ConnectionWrapper(Connection connection)
	{
		this.connection = connection;
	}

	public void setInUse()
	{
		inUse++;
	}

	@Override
	public void run()
	{
		if (inUse > 0)
		{
			return;
		}
		shutdown();
		cancel();
	}

	public void shutdown()
	{
		for (int i = 0; i < statements.length; i++)
		{
			if (statements[i] != null)
			{
				try
				{
					statements[i].statement.close();
				}
				catch (SQLException e)
				{
					LogWrapper.getLogger().log(Level.INFO, null, e);
				}
			}
		}
		try
		{
			connection.close();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, null, e);
		}
	}
	
	@Override
	public void close() throws SQLException
	{
		inUse--;
	}

	public StatementWrapper prepareStatement(QueryWrapper wrapper) throws SQLException
	{
		StatementWrapper statement = statements[wrapper.index];
		if (statement == null)
		{
				statements[wrapper.index] = statement = new StatementWrapper(connection.prepareStatement(wrapper.query), false);
		}
		return statement;
	}


	public StatementWrapper prepareStatement(QueryWrapper wrapper, int returnGeneratedKeys) throws SQLException
	{
		StatementWrapper statement = statements[wrapper.index];
		if (statement == null)
		{
				statements[wrapper.index] = statement = new StatementWrapper(connection.prepareStatement(wrapper.query, returnGeneratedKeys), false);
		}
		return statement;
	}
	
	public StatementWrapper prepareNewStatement(String string) throws SQLException
	{
		return new StatementWrapper(connection.prepareStatement(string), true);
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

		private static int nextId = 0;
		private static synchronized int getNextQueryWrapperId()
		{
			int i = nextId++;
			if (i >= NUM_STATEMENTS)
			{
				throw new RuntimeException("There are too many statements, need to increase ConnectionWrapper.NUM_STATMENTS");
			}
			return i;
		}
	}
	
	/**
	 * I don't like the class, but it was easier than removing all the try () {...} blocks.
	 *
	 */
	public static final class StatementWrapper implements AutoCloseable
	{
		private ResultSet results;
		private PreparedStatement statement;
		private boolean temp;
		
		public StatementWrapper(PreparedStatement statement, boolean temp)
		{
			this.statement = statement;
			this.temp = temp;
		}

		@Override
		public void close()
		{
			if (!temp)
			{
				return;
			}
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to close statement", e);
			}
		}
		
		private ResultSet checkResults(ResultSet newResults) throws SQLException
		{
			if (results != null)
			{
				results.close();
			}
			return results = newResults;
		}

		public void setInt(int i, Integer id) throws SQLException
		{
			statement.setInt(i,  id);
		}

		public ResultSet executeQuery() throws SQLException
		{
			return checkResults(statement.executeQuery());
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
			return checkResults(statement.getGeneratedKeys());
		}
		
		public int getUpdateCount() throws SQLException
		{
			return statement.getUpdateCount();
		}

		public void debug(StringBuilder builder) throws SQLException
		{
			builder.append(" tmp=").append(temp);
			builder.append(" results:");
			if (results == null)
			{
				builder.append("null");
			}
			else if (results.isClosed())
			{
				builder.append("closed");
			}
			else
			{
				builder.append("open");
			}
			builder.append(" query=").append(statement);
			builder.append('\n');
		}
	}

	public boolean isClosed()
	{
		try
		{
			return connection.isClosed();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, null, e);
			return true;
		}
	}

	public synchronized void debug(StringBuilder builder) throws SQLException
	{
		builder.append(" count: ").append(inUse).append('\n');
		for (int i=0;i<statements.length;i++)
		{
			if (statements[i] == null)
			{
				continue;
			}
			builder.append("\tstmt #").append(i);
			statements[i].debug(builder);
		}
	}
}
