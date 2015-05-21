package org.cnv.shr.db.h2;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;

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
			Services.logger.print(e);
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
			Services.logger.print(e);
			return null;
		}
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
			Services.logger.print(e);
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
			Services.logger.print(e);
		}
	}
	
	// Should be in the same transaction...
	public static void updateMachineInfo(
			String ident,
			String name,
			PublicKey[] publicKeys,
			String ip,
			int port,
			int nports)
	{
		Machine machine = getMachine(ident);
		if (machine == null)
		{
			machine = new Machine(ident);
			// By default, we will accept messages from other machines...
			machine.setAllowsMessages(true);
		}
		
		machine.setIp(ip);
		machine.setPort(port);
		machine.setName(name);
		machine.setNumberOfPorts(nports);
		
		try
		{
			machine.save();
			// Is the first of these two really necessary?
			Services.notifications.remoteChanged(machine);
			Services.notifications.remotesChanged();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
			return;
		}
		for (PublicKey key : publicKeys)
		{
			DbKeys.addKey(machine, key);
		}
	}
	
	public static long getTotalNumFiles(Machine machine)
	{
		long returnValue = 0;
		DbIterator<RootDirectory> list = DbRoots.list(machine);
		while (list.hasNext())
		{
			returnValue += list.next().numFiles();
		}
		return returnValue;
	}
	
	public static long getTotalDiskspace(Machine machine)
	{
		long returnValue = 0;
		DbIterator<RootDirectory> list = DbRoots.list(machine);
		while (list.hasNext())
		{
			returnValue += list.next().diskSpace();
		}
		return returnValue;
	}
}
