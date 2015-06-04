package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;

public class DbIterator<T extends DbObject> implements org.cnv.shr.gui.tbl.DbJTable.MyIt<T>
{
	private ResultSet results;
	private DbTables.DbObjects tableInfo;
	private ConnectionWrapper c;
	private DbLocals locals;
	private T next;
	

	public DbIterator(ConnectionWrapper c, DbTables.DbObjects allocator) throws SQLException
	{
		this (c, c.prepareNewStatement("select * from " + allocator.getTableName() + ";").executeQuery(), allocator);
	}
	public DbIterator(ConnectionWrapper c2, ResultSet results, DbTables.DbObjects allocator) throws SQLException
	{
		this(c2, results, allocator, new DbLocals());
	}
	protected DbIterator(ConnectionWrapper c, ResultSet results, DbTables.DbObjects allocator, DbLocals locals) throws SQLException
	{
		this.c = c;
		this.results = results;
		this.tableInfo = allocator;
		this.locals = locals;
		next = findNext();
	}
	private DbIterator() {}

	@Override
	public T next()
	{
		T returnValue = next;
		next = findNext();
		return returnValue;
	}
	
	@Override
	public boolean hasNext()
	{
		if (next != null)
		{
			return true;
		}
		close();
		return false;
	}
	
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("Deletion is not enabled");
	}

	protected T findNext()
	{
		try
		{
			if (!results.next())
			{
				return null;
			}
			
			T t = (T) tableInfo.allocate(results);
			t.fill(c, results, locals);
			return t;
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create a shared file from a record", e);
			return null;
		}
	}
	
	public static final class NullIterator<T extends DbObject> extends DbIterator<T>
	{
		@Override
		public T next()
		{
			throw new UnsupportedOperationException("next not enabled");
		}
		
		@Override
		public boolean hasNext()
		{
			return false;
		}
		
		@Override
		public void remove()
		{
			throw new UnsupportedOperationException("Deletion is not enabled");
		}

		@Override
		protected T findNext() { return null; }
	}

	boolean closed;
	@Override
	public void close()
	{
		if (closed) return;
		closed = true;
		try
		{
			c.close();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close query.", e);
		}
		try
		{
			results.close();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close results.", e);
		}
	}
}
