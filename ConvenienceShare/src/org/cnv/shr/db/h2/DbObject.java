package org.cnv.shr.db.h2;

import java.sql.Connection;
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
	
	public abstract void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException;
	
	public void setId(int i)
	{
		throw new RuntimeException("Fix this.");
	}
	
	public T getId()
	{
		return id;
	}

	public final boolean save()    throws SQLException { return save(Services.h2DbCache.getConnection()); }
	public final void    pull()    throws SQLException {      /*  pull(Services.h2DbCache.getConnection()); */}
	
	public abstract boolean save(Connection c) throws SQLException;
}
