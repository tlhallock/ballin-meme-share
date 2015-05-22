package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
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
	private static final QueryWrapper SELECT2   = new QueryWrapper("select * from SFILE where F_ID=?;");
	private static final QueryWrapper DELETE1   = new QueryWrapper("delete from SFILE where F_ID=?;");
	private static final QueryWrapper SELECT1   = new QueryWrapper("select * from SFILE where PELEM=? and ROOT=?;");
	private static final QueryWrapper UNCHECKED = new QueryWrapper("select * from SFILE join ROOT on SFILE.ROOT=ROOT.R_ID where ROOT.IS_LOCAL and SFILE.CHKSUM=NULL limit 1;");

	public static SharedFile getFile(RootDirectory root, PathElement element)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1);)
		{
			stmt.setLong(1, element.getId());
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

	public static LocalFile getUnChecksummedFile()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(UNCHECKED);)
		{
			ResultSet executeQuery = stmt.executeQuery();
			if (!executeQuery.next())
			{
				return null;
			}
			DbObject allocate = DbTables.DbObjects.LFILE.allocate(executeQuery);
			allocate.fill(c, executeQuery, new DbLocals());
			return (LocalFile) allocate;
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
			return null;
		}
	}
	
	public static void delete(SharedFile f)
	{
		// Delete from pending too...
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1);)
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
		return null;
	}

	public static SharedFile getFile(int int1)
	{
		// Delete from pending too...
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2);)
		{
			stmt.setInt(1, int1);
			ResultSet executeQuery = stmt.executeQuery();
			if (!executeQuery.next())
			{
				return null;
			}
			DbObject allocated = DbTables.DbObjects.RFILE.allocate(executeQuery);
			allocated.fill(c, executeQuery, new DbLocals());
			return (SharedFile) allocated;
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
			return null;
		}
	}
}
