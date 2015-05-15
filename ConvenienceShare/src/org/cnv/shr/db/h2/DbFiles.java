package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public class DbFiles
{
	public static SharedFile getFile(RootDirectory root, PathElement element)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				"select * from SFILE where PELEM=? and ROOT=?;");)
		{
			stmt.setInt(1, element.getId());
			stmt.setInt(2, root.getId());
			
			ResultSet executeQuery = stmt.executeQuery();
			if (!executeQuery.next())
			{
				return null;
			}
			DbObjects lfile = root.isLocal() ? DbTables.DbObjects.LFILE : DbTables.DbObjects.RFILE;
			DbObject allocate = lfile.allocate(executeQuery);
			allocate.fill(c, executeQuery, new DbLocals().setObject(element).setObject(root));
			return (SharedFile) allocate;
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
			return null;
		}
	}
	
	public static void delete(SharedFile f)
	{
		Connection c = Services.h2DbCache.getConnection();
		// Delete from pending too...
		try (PreparedStatement stmt = c.prepareStatement("delete from SFILE where F_ID=?;");)
		{
			stmt.setInt(1, f.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
	
	public static RemoteFile getFile(RemoteDirectory root, PathElement element)
	{
		return (RemoteFile) getFile((RootDirectory) root, element);
	}
	public static LocalFile getFile(LocalDirectory root, PathElement element)
	{
		return (LocalFile) getFile((RootDirectory) root, element);
	}

	public static SharedFile getFile(String checksum, long fileSize)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
