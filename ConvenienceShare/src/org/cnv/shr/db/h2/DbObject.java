package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

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

	public final boolean tryToSave()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
		{
			return save(c);
		}
		catch (SQLException e)
		{
				LogWrapper.getLogger().log(Level.INFO, "Unable to save object of type " + getClass().getName(), e);
				return false;
		}
	}

	public abstract boolean save(ConnectionWrapper c) throws SQLException;
}
