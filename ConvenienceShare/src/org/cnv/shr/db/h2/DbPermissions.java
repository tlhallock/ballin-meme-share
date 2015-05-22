package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;

public class DbPermissions
{
	private static final QueryWrapper SELECT1 = new QueryWrapper("select IS_SHARING from SHARE_ROOT where RID=? and MID=?;");
	private static final QueryWrapper MERGE1  = new QueryWrapper("merge into SHARE_ROOT key(RID, MID) values (?, ?, ?);");

	// Needs to work for remote roots too: ie add a listener...

	public static void share(Machine machine, RootDirectory root, SharingState share)
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
			Services.logger.print(e);
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
				return SharingState.NOT_SET;
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
			Services.logger.print(e);
		}
		return SharingState.DO_NOT_SHARE;
	}
	
	
	public enum SharingState
	{
		DO_NOT_SHARE(0),
		SHARE_PATHS (1),
		DOWNLOADABLE(2),
		NOT_SET     (3),
		
		;
		
		int state;
		
		SharingState(int i)
		{
			this.state = i;
		}
		
		public boolean is(int i)
		{
			return state == i;
		}
		
		public boolean canList()
		{
			return this.equals(DOWNLOADABLE) || this.equals(SHARE_PATHS);
		}
		
		public boolean canDownload()
		{
			return this.equals(DOWNLOADABLE);
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
			return SharingState.NOT_SET;
		}

		public int getDbValue()
		{
			return state;
		}
	}
}
