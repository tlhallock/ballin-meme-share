package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;

public class DbFiles
{
	public static LocalFile getFile(RootDirectory local, PathElement element)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				"select * from SFILE where PELEM=? and ROOT=?;");)
		{
			stmt.setInt(1, element.getId());
			stmt.setInt(2, local.getId());
			
			ResultSet executeQuery = stmt.executeQuery();
			if (!executeQuery.next())
			{
				return null;
			}
			DbObject allocate = DbTables.DbObjects.LFILE.allocate(executeQuery);
			allocate.fill(c, executeQuery, new DbLocals().setObject(element).setObject(local));
			return (LocalFile) allocate;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static RemoteFile getRemoteFile(RootDirectory directory, String path)
	{
		return null;
	}
}
