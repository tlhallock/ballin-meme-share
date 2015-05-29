package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
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

	public static SharingState isSharing(Machine machine, RootDirectory root)
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
				return SharingState.UNKOWN;
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
		return SharingState.DO_NOT_SHARE;
	}
	
	
	public enum SharingState
	{
		DO_NOT_SHARE(0),
		SHARE_PATHS (1),
		DOWNLOADABLE(2),
		DEFAULT     (3),
		UNKOWN      (4),
		
		;
		
		int state;
		
		SharingState(int i)
		{
			this.state = i;
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
			return SharingState.UNKOWN;
		}

		public int getDbValue()
		{
			return state;
		}
	}
}
