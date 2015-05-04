package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;

public abstract class DbObject
{
	protected Integer id;
	
	public DbObject(Integer id)
	{
		this.id = id;
	}
	
	public abstract void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException;
	protected abstract PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException;
	
	public final void setId(int i)
	{
		throw new RuntimeException("Fix this.");
	}
	
	public Integer getId()
	{
		return id;
	}

	public final void add()    throws SQLException {    add(Services.h2DbCache.getConnection()); }
	public final void update() throws SQLException { update(Services.h2DbCache.getConnection()); }
	public final void delete() throws SQLException { delete(Services.h2DbCache.getConnection()); }

	public final void add(Connection c) throws SQLException
	{
		
	}
	
	public final void update(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = createPreparedUpdateStatement(c);)
		{
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				id = generatedKeys.getInt(1);
			}
		}
	}
	
	public final void delete(Connection c) throws SQLException
	{
		
	}
}
