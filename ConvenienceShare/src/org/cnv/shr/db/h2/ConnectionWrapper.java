package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;

import org.cnv.shr.dmn.Services;

public class ConnectionWrapper extends TimerTask implements AutoCloseable
{
	private static final int NUM_STATEMENTS = 100;
	private StatementWrapper[] statements = new StatementWrapper[NUM_STATEMENTS];
	
	private Connection connection;
	private boolean inUse;
	
	public ConnectionWrapper(Connection connection)
	{
		this.connection = connection;
	}

	public void setInUse()
	{
		inUse = true;
	}

	@Override
	public void run()
	{
		if (inUse)
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
					e.printStackTrace();
				}
			}
		}
		try
		{
			connection.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() throws SQLException
	{
		inUse = false;
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
			return nextId++;
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
				Services.logger.print(e);
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
	}
}
