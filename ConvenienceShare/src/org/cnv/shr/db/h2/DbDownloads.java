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
	private static final QueryWrapper SELECT1 = new QueryWrapper("select Q_ID from DOWNLOAD where FID=?;");

	public static boolean hasPendingDownload(SharedFile remoteFile)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1);)
		{
			
			stmt.setInt(1, remoteFile.getId());
			ResultSet executeQuery = stmt.executeQuery();
			return executeQuery.next();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to see if we want to download " + remoteFile, e);
			return false;
		}
	}

    public static void clearCompleted()
    {
//		DbChunks.allChunksDone(download);
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1))
		{
			stmt.setInt(1, DownloadState.ALL_DONE.toInt());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to clear completed download chunks", e);
		}
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

	public static Download getDownload(int parseInt)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
		{
			stmt.setInt(1, parseInt);
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				DbObject allocate = DbObjects.CHUNK.allocate(executeQuery);
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
			return new DbIterator<>(Services.h2DbCache.getThreadConnection(), DbObjects.PENDING_DOWNLOAD);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get downloads", e);
			return new DbIterator.NullIterator<>();
		}
	}
}
