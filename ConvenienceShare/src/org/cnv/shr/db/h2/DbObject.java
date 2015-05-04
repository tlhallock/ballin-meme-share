package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class DbObject
{
	protected Integer id;
	
	public DbObject(Integer id)
	{
		this.id = id;
	}
	
	public abstract void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException;
	protected abstract PreparedStatement createPreparedUpdateStatement(Connection c);
	
	public Integer getId()
	{
		return id;
	}
	

	public final void add(Connection c) throws SQLException
	{
		
	}
	
	public final void update(Connection c) throws SQLException
	{
	}
	
	public final void delete(Connection c) throws SQLException
	{
		
	}
}
