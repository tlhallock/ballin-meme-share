package org.cnv.shr.db.h2;

import java.security.PublicKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class DbMachines
{
	private static final QueryWrapper DELETE1   = new QueryWrapper("delete MACHINE where M_ID=?;");
	private static final QueryWrapper SELECT3   = new QueryWrapper("select M_ID from MACHINE where IDENT = ?");
	private static final QueryWrapper SELECT2   = new QueryWrapper("select * from MACHINE where IDENT = ?");
	private static final QueryWrapper SELECT1   = new QueryWrapper("select * from MACHINE where MACHINE.IS_LOCAL = false");
	private static final QueryWrapper SELECT1_5 = new QueryWrapper("select * from MACHINE");


	public static DbIterator<Machine> listMachines()
	{
		ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
		try
		{
			return new DbIterator<>(c,
					c.prepareStatement(SELECT1_5).executeQuery(),
					DbTables.DbObjects.RMACHINE);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list machines", e);
			return new DbIterator.NullIterator<>();
		}
	}
	
	public static DbIterator<Machine> listRemoteMachines()
	{
		ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
		try
		{
			return new DbIterator<>(c,
					c.prepareStatement(SELECT1).executeQuery(),
					DbTables.DbObjects.RMACHINE);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list remotes", e);
			return new DbIterator.NullIterator<>();
		}
	}
	
	public static Machine getMachine(String identifier)
	{
		if (identifier.equals(Services.localMachine.getIdentifier()))
		{
			return Services.localMachine;
		}
		
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to get machine by identifier", e);
			return null;
		}
	}

	public static Integer getMachineId(String identifier)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT3))
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to get machine id for " + identifier, e);
			return null;
		}
	}

	public static void delete(Machine remote)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1))
		{
			stmt.setInt(1, remote.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete machine " + remote, e);
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

		machine.tryToSave();
		// Is the first of these two really necessary?
		Services.notifications.remoteChanged(machine);
		Services.notifications.remotesChanged();
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
