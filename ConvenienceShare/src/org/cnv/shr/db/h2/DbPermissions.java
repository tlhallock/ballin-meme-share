
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class DbPermissions
{
	private static final QueryWrapper SELECT1    = new QueryWrapper("select IS_SHARING from SHARE_ROOT where RID=? and MID=?;");
	private static final QueryWrapper MERGE1     = new QueryWrapper("merge into SHARE_ROOT key(RID, MID) values (?, ?, ?);");

	// Needs to work for remote roots too: ie add a listener...

	public static void setSharingState(Machine machine, RootDirectory root, SharingState share)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(MERGE1))
		{
			int ndx = 1;
			stmt.setInt(ndx++, root.getId());
			stmt.setInt(ndx++, machine.getId());
			stmt.setInt(ndx++, share.state);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "unable to set share permissions with " + machine, e);
		}
	}

	private static SharingState isSharing(Machine machine, RootDirectory root)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1))
		{
			int ndx = 1;
			stmt.setInt(ndx++, root.getId());
			stmt.setInt(ndx++, machine.getId());
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (!executeQuery.next())
				{
					return null;
				}
				int dbValue = executeQuery.getInt(1);
				for (SharingState state : SharingState.values())
				{
					if (state.state == dbValue)
					{
						return state;
					}
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to check sharing with " + machine, e);
		}
		return null;
	}
	
	public static SharingState getCurrentPermissions(Machine machine)
	{
		SharingState current = null;
		SharingState other = machine.sharingWithOther();
		if (current == null || other != null && other.isMoreRestrictiveThan(current))
		{
			current = other;
		}
		return current == null ? SharingState.valueOf(Services.settings.defaultPermission.get()) : current;
	}

	public static SharingState getCurrentPermissions(RemoteDirectory root)
	{
		SharingState current = isSharing(Services.localMachine, root);
		return current == null ? SharingState.DO_NOT_SHARE : current;
	}
	
	public static SharingState getCurrentPermissions(Machine machine, LocalDirectory root)
	{
		SharingState current = null;
		SharingState other = machine.sharingWithOther();
		if (current == null || other != null && other.isMoreRestrictiveThan(current))
		{
			current = other;
		}
		other = root.getDefaultSharingState();
		if (current == null || other != null && other.isMoreRestrictiveThan(current))
		{
			current = other;
		}
		other = isSharing(machine, root);
		if (current == null || other != null && other.isMoreRestrictiveThan(current))
		{
			current = other;
		}
		return current == null ? SharingState.valueOf(Services.settings.defaultPermission.get()) : current;
	}
}
