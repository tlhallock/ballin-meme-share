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
	private static final QueryWrapper SELECT1 = new QueryWrapper("select IS_SHARING from SHARE_ROOT where RID=? and MID=?;");
	private static final QueryWrapper MERGE1  = new QueryWrapper("merge into SHARE_ROOT key(RID, MID) values (?, ?, ?);");

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
			ResultSet executeQuery = stmt.executeQuery();
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
		if (current == null || (other != null && other.getDbValue() < current.getDbValue()))
		{
			current = other;
		}
		return current == null ? SharingState.DO_NOT_SHARE : current;
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
		if (current == null || (other != null && other.getDbValue() < current.getDbValue()))
		{
			current = other;
		}
		other = root.getDefaultSharingState();
		if (current == null || (other != null && other.getDbValue() < current.getDbValue()))
		{
			current = other;
		}
		other = isSharing(machine, root);
		if (current == null || (other != null && other.getDbValue() < current.getDbValue()))
		{
			current = other;
		}
		return current == null ? SharingState.DO_NOT_SHARE : current;
	}
	
	
	
	
	
	
	
	
	public enum SharingState
	{
		DO_NOT_SHARE(1, false, false),
		SHARE_PATHS (2,  true, false),
		DOWNLOADABLE(3,  true,  true),
//		DEFAULT     (4, false, false),
		
		;
		
		boolean canList;
		boolean canDownload;
		int state;
		
		SharingState(int i, boolean cl, boolean cd)
		{
			this.state = i;
			this.canList = cl;
			this.canDownload = cd;
		}
		
		public String humanReadable()
		{
			return name();
		}
		
		public boolean is(int i)
		{
			return state == i;
		}
		
		public static SharingState get(int dbValue)
		{
			for (SharingState s : values())
			{
				if (s.state == dbValue)
				{
					return s;
				}
			}
			return null;
		}

		public int getDbValue()
		{
			return state;
		}

		public boolean downloadable()
		{
			return canDownload;
		}

		public boolean listable()
		{
			return canList;
		}
	}
}
