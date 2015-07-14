
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
	private static final QueryWrapper DELETE2   = new QueryWrapper("delete from SFILE where not exists (select RID from ROOT_CONTAINS where ROOT_CONTAINS.RID = SFILE.ROOT and ROOT_CONTAINS.PELEM=SFILE.PELEM);");
	private static final QueryWrapper SELECT1   = new QueryWrapper("select * from SFILE where PELEM=? and ROOT=?;");
	private static final QueryWrapper SELECT3   = new QueryWrapper("select * from SFILE join ROOT on SFILE.ROOT=ROOT.R_ID where CHKSUM=? and FSIZE=? and ROOT.IS_LOCAL LIMIT 1;");
	private static final QueryWrapper UNCHECKED = new QueryWrapper("select * from SFILE join ROOT on SFILE.ROOT=ROOT.R_ID where ROOT.IS_LOCAL and SFILE.CHKSUM IS NULL and SFILE.MODIFIED < ? LIMIT 200;");
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

//	public static LocalFile getFile(String checksum, long fileSize)
//	{
//		return getFile(checksum);
//	}
	public static LocalFile getFile(String checksum, long fileSize)
	{
		// Delete from pending too...
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT3);)
		{
			stmt.setString(1, checksum);
			stmt.setLong(2, fileSize);
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
	


	public static DbIterator<LocalFile> getSomeUnchecksummedFiles()
	{
		ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
		try
		{
			StatementWrapper prepareStatement = c.prepareStatement(UNCHECKED);
			prepareStatement.setLong(1, System.currentTimeMillis() - 5 * 60 * 1000);
			return new DbIterator<LocalFile>(c, prepareStatement.executeQuery(), DbTables.DbObjects.LFILE);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list files without a checksum", e);
			return new NullIterator<LocalFile>();
		}
	}

	public static DbIterator<LocalFile> getChecksummedFiles()
	{
		try 
		{
			ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			StatementWrapper prepareStatement = c.prepareStatement(CHECKED);
			prepareStatement.setFetchSize(50);
			return new DbIterator<LocalFile>(c, prepareStatement.executeQuery(), DbTables.DbObjects.LFILE);
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
			StatementWrapper prepareStatement = c.prepareStatement(ALL);
			prepareStatement.setFetchSize(50);
			return new DbIterator<LocalFile>(c, prepareStatement.executeQuery(), DbTables.DbObjects.LFILE);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list all local files", e);
			return new NullIterator<>();
		}
	}

	public static void cleanFiles()
	{
		
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				 StatementWrapper wrapper = c.prepareStatement(DELETE2);)
		{
			wrapper.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable clean unused files.", e);
		}
	}
}
