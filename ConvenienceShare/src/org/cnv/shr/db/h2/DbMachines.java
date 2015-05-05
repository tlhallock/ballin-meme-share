package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;

public class DbMachines
{
	public static DbIterator<Machine> listRemoteMachines()
	{
		Connection c = Services.h2DbCache.getConnection();
		try
		{
			return new DbIterator<>(c,
					c.prepareStatement("select * from MACHINE where MACHINE.IS_LOCAL = false").executeQuery(),
					DbTables.DbObjects.RMACHINE);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new DbIterator.NullIterator<>();
		}
	}
	
	public static Machine getMachine(String identifier)
	{
		Connection c = Services.h2DbCache.getConnection();
		return null;
	}
	public static Machine getMachine(int machineId)
	{
		Connection c = Services.h2DbCache.getConnection();
		return null;
	}

	public static Integer getMachineId(String identifier)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select M_ID from MACHINE where IDENT = ?"))
		{
			stmt.setString(1, identifier);
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getInt(1);
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
