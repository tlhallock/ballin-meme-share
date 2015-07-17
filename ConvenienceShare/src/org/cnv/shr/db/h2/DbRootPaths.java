package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public class DbRootPaths
{
	public static final int ROOT_PATH_LENGTH = 256;

	private static final QueryWrapper INSERT        = new QueryWrapper("merge into ROOT_PATH key (PREV, VALUE) values (DEFAULT, ?, ?);");
	private static final QueryWrapper GET_PATH      = new QueryWrapper("select PREV, VALUE from ROOT_PATH where RP_ID=?;");
	private static final QueryWrapper GET_ID        = new QueryWrapper("select RP_ID from ROOT_PATH where PREV=? and VALUE=?;");
	private static final QueryWrapper RM            = new QueryWrapper("delete from ROOT_PATH where RP_ID=?;");
	private static final QueryWrapper COUNT         = new QueryWrapper("select count(RP_ID) from ROOT_PATH;");
	private static final QueryWrapper COUNT_OTHERS  = new QueryWrapper("select count(RP_ID) from ROOT_PATH where PREV=?;");
	

	public static int getRootPath(String str)
	{
		LinkedList<String> elements = new LinkedList<>();
		int start = 0;
		int end = Math.min(str.length(), ROOT_PATH_LENGTH);
		while (start < str.length())
		{
			elements.add(str.substring(start, end));
			start = end;
			end = Math.min(str.length(), end + ROOT_PATH_LENGTH);
		}
		
		int parent = 0;

		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper query  = wrapper.prepareStatement(GET_ID);
				 StatementWrapper insert = wrapper.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);)
		{
			while (!elements.isEmpty())
			{
				String current = elements.removeFirst();
				query.setInt(1, parent);
				query.setString(2, current);

				try (ResultSet results = query.executeQuery();)
				{
					if (!results.next())
					{
						elements.addFirst(current);
						break;
					}
					parent = results.getInt(1);
				}
			}
			while (!elements.isEmpty())
			{
				String current = elements.removeFirst();
				insert.setInt(1, parent);
				insert.setString(2, current);
				insert.executeUpdate();
				
				try (ResultSet generatedKeys = insert.getGeneratedKeys();)
				{
					if (!generatedKeys.next())
					{
						throw new RuntimeException("Unable to get new key!");
					}
					parent = generatedKeys.getInt(1);
				}
			}
			
			return parent;
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create new root path.", e);
		}
		return -1;
	}
	
	public static String getRootPath(int id)
	{
		RStringBuilder builder = new RStringBuilder();
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper query = wrapper.prepareStatement(GET_PATH);)
		{
			do
			{
				query.setInt(1, id);
				try (ResultSet results = query.executeQuery();)
				{
					if (!results.next())
					{
						throw new RuntimeException("No path exists for " + id);
					}
					
					id = results.getInt(1);
					builder.preppend(results.getString(2));
				}
			} while (id > 0);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read root path.", e);
		}
		return builder.toString();
	}
	
	public static void removeRootPath(int id)
	{
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper query    = wrapper.prepareStatement(GET_PATH);
				 StatementWrapper remove   = wrapper.prepareStatement(RM);
				 StatementWrapper count    = wrapper.prepareStatement(COUNT_OTHERS);)
		{
			do
			{
				query.setInt(1, id);
				try (ResultSet results = query.executeQuery();)
				{
					if (!results.next())
					{
						throw new RuntimeException("No path exists for " + id);
					}

					int parent = results.getInt(1);
					
					remove.setInt(1, id);
					remove.executeUpdate();

					count.setInt(1, parent);
					try (ResultSet others = count.executeQuery();)
					{
						if (!others.next())
						{
							throw new RuntimeException("Unable able to determine if others use " + parent);
						}
						int numOthers = others.getInt(1);
						if (numOthers > 0)
						{
							// some other path uses this parent...
							return;
						}
					}
					
					id = parent;
				}
			} while (id > 0);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read root path.", e);
		}
	}
	
	public static int getNumRootPathElements() throws SQLException
	{
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper query = wrapper.prepareStatement(COUNT);
				 ResultSet results = query.executeQuery();)
		{
			if (!results.next())
			{
				throw new RuntimeException("Unable to execute query.");
			}
			return results.getInt(1);
		}
	}

	public static void main(String[] args)
	{
		
	}
}
