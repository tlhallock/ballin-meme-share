
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
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.LogWrapper;

public class DbDownloads
{
	private static final QueryWrapper SELECT2 = new QueryWrapper("select * from DOWNLOAD where Q_ID=?;");
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete from DOWNLOAD where DSTATE=?;");
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from CHUNK join DOWNLOAD on DID = Q_ID where DSTATE=?;");
	private static final QueryWrapper DELETE3 = new QueryWrapper("delete from DOWNLOAD where Q_ID in (select Q_ID from DOWNLOAD join SFILE on FID = F_ID where CHKSUM IS NULL or trim(CHKSUM)='');");
	private static final QueryWrapper SELECTID = new QueryWrapper("select Q_ID from DOWNLOAD where FID=?;");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select * from Download where DSTATE "
//			+ "in (" + collect(new int[] {
//			DownloadState.QUEUED.toInt(),
//			DownloadState.ALLOCATING.toInt(),
//			DownloadState.RECOVERING.toInt(),
//			DownloadState.REQUESTING_METADATA.toInt(),
//			DownloadState.RECEIVING_METADATA.toInt(),
//			DownloadState.DOWNLOADING.toInt(),
//		}) + ")"
			  + " <> " + DownloadState.ALL_DONE.toInt()
			  + " and DSTATE <> " + DownloadState.FAILED.toInt()
				+ " group by PRIORITY, Q_ID order by PRIORITY desc, ADDED asc;");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select * from Download                group by PRIORITY, Q_ID order by PRIORITY desc, ADDED asc, DSTATE asc;");
	private static final QueryWrapper COUNT = new QueryWrapper("select count(Q_ID) from Download;");

	public static Integer getPendingDownloadId(SharedFile remoteFile)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECTID);)
		{

			stmt.setInt(1, remoteFile.getId());
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (executeQuery.next())
				{
					return executeQuery.getInt(1);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to see if we want to download " + remoteFile, e);
		}
		return null;
	}

	public static void clearCompleted()
	{
//		DbChunks.allChunksDone(download);
//		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
//				StatementWrapper stmt = c.prepareStatement(DELETE1))
//		{
//			stmt.setInt(1, DownloadState.ALL_DONE.toInt());
//			stmt.execute();
//		}
//		catch (SQLException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to clear completed download chunks", e);
//		}
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE2))
		{
			stmt.setInt(1, DownloadState.ALL_DONE.toInt());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to clear completed downloads", e);
		}
	}
	public static void cleanUnchecksummedFiles()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE3))
		{
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to clear completed downloads", e);
		}
	}

	public static Download getDownload(int parseInt)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
		{
			stmt.setInt(1, parseInt);
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (!executeQuery.next())
				{
					return null;
				}
				DbObject allocate = DbObjects.PENDING_DOWNLOAD.allocate(executeQuery);
				allocate.fill(c, executeQuery, new DbLocals());
				return (Download) allocate;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get download " + parseInt, e);
		}
		return null;
	}


	public static DbIterator<Download> listDownloads()
	{
		try
		{
			ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
			return new DbIterator<Download>(connection, connection.prepareStatement(DbDownloads.SELECT3).executeQuery(), DbObjects.PENDING_DOWNLOAD);
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list downloads.", e1);
			return new DbIterator.NullIterator<>();
		}
	}

	public static DbIterator<Download> listPendingDownloads()
	{
		try
		{
			ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
			StatementWrapper prepareStatement = connection.prepareStatement(DbDownloads.SELECT1);
			return new DbIterator<Download>(connection, prepareStatement.executeQuery(), DbObjects.PENDING_DOWNLOAD);
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list pending downloads.", e1);
			return new DbIterator.NullIterator<>();
		}
	}

	private static String collect(int[] inStates)
	{
		StringBuilder builder = new StringBuilder();

		if (inStates.length == 0)
		{
			LogWrapper.getLogger().info("Cannot list downloads without having a set of states to be in.");
			return null;
		}
		
		builder.append(inStates[0]);
		for (int i=1;i<inStates.length;i++)
		{
			builder.append(", ");
			builder.append(inStates[i]);
		}
		
		return builder.toString();
	}

	public static int getNumDownloads()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				 StatementWrapper stmt = c.prepareStatement(COUNT);
				 ResultSet executeQuery = stmt.executeQuery();)
		{
			if (executeQuery.next())
			{
				return executeQuery.getInt(1);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable get number of downloads.", e);
		}
		return 0;
	}
}
