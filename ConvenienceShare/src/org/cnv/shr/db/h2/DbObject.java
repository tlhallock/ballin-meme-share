package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;

public abstract class DbObject<T>
{
	protected T id;
	
	public DbObject(T id)
	{
		this.id = id;
	}
	
	public abstract void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException;
	
	public void setId(int i)
	{
		throw new RuntimeException("Fix this.");
	}
	
	public T getId()
	{
		return id;
	}

	public final boolean save() throws SQLException
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
		{
			return save(c);
		}
	}

	public abstract boolean save(ConnectionWrapper c) throws SQLException;
}
