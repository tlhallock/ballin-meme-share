
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



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
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
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
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (executeQuery.next())
				{
					return executeQuery.getInt(1);
				}
				else
				{
					return null;
				}
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
			PublicKey key,
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

		if (!machine.tryToSave())
		{
			LogWrapper.getLogger().info("Unable to save new machine. This is possibly because the machine changed identifier.");
			throw new RuntimeException("Unable to save new machine.");
		}
		// Is the first of these two really necessary?
		Services.notifications.remoteChanged(machine);
		Services.notifications.remotesChanged();
		if (key != null)
		{
			DbKeys.addKey(machine, key);
		}
	}

	public static long getTotalNumFiles(Machine machine)
	{
		long returnValue = 0;
		try (DbIterator<RootDirectory> list = DbRoots.list(machine);)
		{
			while (list.hasNext())
			{
				returnValue += list.next().numFiles();
			}
		}
		return returnValue;
	}
	
	public static long getTotalDiskspace(Machine machine)
	{
		long returnValue = 0;
		try (DbIterator<RootDirectory> list = DbRoots.list(machine);)
		{
			while (list.hasNext())
			{
				returnValue += list.next().diskSpace();
			}
		}
		return returnValue;
	}
}
