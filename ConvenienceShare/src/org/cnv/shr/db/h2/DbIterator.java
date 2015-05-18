package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.cnv.shr.dmn.Services;

public class DbIterator<T extends DbObject> implements Iterator<T>
{
	private ResultSet results;
	private DbTables.DbObjects tableInfo;
	private Connection c;
	private DbLocals locals;
	private T next;
	

	public DbIterator(Connection c, DbTables.DbObjects allocator) throws SQLException
	{
		this (c, c.prepareStatement("select * from " + allocator.getTableName() + ";").executeQuery(), allocator);
	}
	public DbIterator(Connection c, ResultSet results, DbTables.DbObjects allocator) throws SQLException
	{
		this(c, results, allocator, new DbLocals());
	}
	protected DbIterator(Connection c, ResultSet results, DbTables.DbObjects allocator, DbLocals locals) throws SQLException
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
		try
		{
			results.getStatement().close();
		}
		catch (SQLException e)
		{
			Services.logger.println("Unable to close query.");
			Services.logger.print(e);
		}
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
			Services.logger.println("Unable to create a shared file from a record");
			Services.logger.print(e);
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

		protected T findNext() { return null; }
	}
}
