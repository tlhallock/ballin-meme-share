package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;

public class DbPermissions
{
	public static void share(Machine machine, RootDirectory root, SharingState share)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("merge into SHARE_ROOT key(RID, MID) values (?, ?, ?);"))
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
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select IS_SHARING from SHARE_ROOT where RID=? and MID=?;"))
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
		return SharingState.INVISIBLE;
	}
	
	
	public enum SharingState
	{
		INVISIBLE   (0),
		VISIBLE     (1),
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
	}
}
