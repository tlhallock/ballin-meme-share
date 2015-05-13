package org.cnv.shr.db.h2;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
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
		if (identifier.equals(Services.localMachine.getIdentifier()))
		{
			return Services.localMachine;
		}
		
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select * from MACHINE where IDENT = ?"))
		{
			stmt.setString(1, identifier);
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				Machine machine = (Machine) DbObjects.RMACHINE.allocate(executeQuery);
				machine.fill(c, executeQuery, new DbLocals());
				return machine;
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

	public static void delete(Machine remote)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("delete MACHINE where M_ID=?;"))
		{
			stmt.setInt(1, remote.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	
	// Should be in the same transaction...
	public static void updateMachineInfo(Machine claimedMachine, PublicKey[] publicKeys, String ip)
	{
		if (ip != null)
		{
			claimedMachine.setIp(ip);
		}
		Machine machine = getMachine(claimedMachine.getIdentifier());
		if (machine == null)
		{
			machine = claimedMachine;
		}
		machine.setIp(claimedMachine.getIp());
		machine.setPort(claimedMachine.getPort());
		machine.setName(claimedMachine.getName());
		machine.setNumberOfPorts(claimedMachine.getNumberOfPorts());
		
		if (publicKeys != null)
		{
			for (PublicKey key : publicKeys)
			{
				DbKeys.addKey(machine, key);
			}
		}
		
		try
		{
			machine.save();
			Services.notifications.remotesChanged();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
}
