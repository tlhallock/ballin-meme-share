
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;

// From documentation, I should look at:
//stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
//    java.sql.ResultSet.CONCUR_READ_ONLY);
//stmt.setFetchSize(Integer.MIN_VALUE);


public class DbIterator<T extends DbObject> implements CloseableIterator<T>
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
			// this is why the go negative sometimes...
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
