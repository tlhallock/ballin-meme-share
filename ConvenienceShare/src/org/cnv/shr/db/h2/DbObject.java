package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
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
	
	public void setId(int i)
	{
		throw new RuntimeException("Fix this.");
	}
	
	public Integer getId()
	{
		return id;
	}

	public final boolean save()    throws SQLException { return save(Services.h2DbCache.getConnection()); }
	public final void    pull()    throws SQLException {      /*  pull(Services.h2DbCache.getConnection()); */}
	
	public final boolean save(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = createPreparedUpdateStatement(c);)
		{
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				id = generatedKeys.getInt(1);
				return true;
			}
			return false;
		}
	}
}
