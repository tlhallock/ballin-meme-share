package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbIterator.NullIterator;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.LogWrapper;

public class DbFiles
{
	private static final QueryWrapper SELECT2   = new QueryWrapper("select * from SFILE where F_ID=?;");
	private static final QueryWrapper DELETE1   = new QueryWrapper("delete from SFILE where F_ID=?;");
	private static final QueryWrapper SELECT1   = new QueryWrapper("select * from SFILE where PELEM=? and ROOT=?;");
	private static final QueryWrapper SELECT3   = new QueryWrapper("select * from SFILE where CHKSUM=? join ROOT on SFILE.ROOT=ROOT.R_ID where ROOT.IS_LOCAL;");
	private static final QueryWrapper UNCHECKED = new QueryWrapper("select * from SFILE join ROOT on SFILE.ROOT=ROOT.R_ID where ROOT.IS_LOCAL and SFILE.CHKSUM IS NULL limit 1;");
	private static final QueryWrapper CHECKED   = new QueryWrapper("select * from SFILE join ROOT on SFILE.ROOT=ROOT.R_ID where ROOT.IS_LOCAL and SFILE.CHKSUM IS NOT NULL;");
	private static final QueryWrapper ALL       = new QueryWrapper("select * from SFILE join ROOT on SFILE.ROOT=ROOT.R_ID where ROOT.IS_LOCAL;");

	public static SharedFile getFile(RootDirectory root, PathElement element)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1);)
		{
			stmt.setLong(1, element.getId());
			stmt.setInt(2, root.getId());

			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (!executeQuery.next())
				{
					return null;
				}
				DbObjects lfile = root.isLocal() ? DbTables.DbObjects.LFILE : DbTables.DbObjects.RFILE;
				DbObject allocate = lfile.allocate(executeQuery);
				allocate.fill(c, executeQuery, new DbLocals().setObject(element).setObject(root));
				return (SharedFile) allocate;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get file " + element, e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete file " + f, e);
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

	public static LocalFile getFile(String checksum, long fileSize)
	{
		return getFile(checksum);
	}

	public static SharedFile getFile(int int1)
	{
		// Delete from pending too...
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection(); 
				StatementWrapper stmt = c.prepareStatement(SELECT2);)
		{
			stmt.setInt(1, int1);
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (!executeQuery.next())
				{
					return null;
				}
				DbObject allocated = DbTables.DbObjects.RFILE.allocate(executeQuery);
				allocated.fill(c, executeQuery, new DbLocals());
				return (SharedFile) allocated;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get file by id " + int1, e);
			return null;
		}
	}

	public static LocalFile getFile(String checksum)
	{
		// Delete from pending too...
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT3);)
		{
			stmt.setString(1, checksum);
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (!executeQuery.next())
				{
					return null;
				}
				DbObject allocated = DbTables.DbObjects.LFILE.allocate(executeQuery);
				allocated.fill(c, executeQuery, new DbLocals());
				return (LocalFile) allocated;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get file by checksum " + checksum, e);
			return null;
		}
	}
	


	public static LocalFile getUnChecksummedFile()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(UNCHECKED);)
		{
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (!executeQuery.next())
				{
					return null;
				}
				DbObject allocate = DbTables.DbObjects.LFILE.allocate(executeQuery);
				allocate.fill(c, executeQuery, new DbLocals());
				return (LocalFile) allocate;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list files without a checksum", e);
			return null;
		}
	}

	public static DbIterator<LocalFile> getChecksummedFiles()
	{
		try 
		{
			ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			return new DbIterator<LocalFile>(c, c.prepareStatement(CHECKED).executeQuery(), DbTables.DbObjects.LFILE);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list files without a checksum", e);
			return new NullIterator<>();
		}
	}

		public static DbIterator<LocalFile> listAllLocalFiles()
		{
			try 
			{
				ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				return new DbIterator<LocalFile>(c, c.prepareStatement(ALL).executeQuery(), DbTables.DbObjects.LFILE);
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to list all local files", e);
				return new NullIterator<>();
			}
	}
}
