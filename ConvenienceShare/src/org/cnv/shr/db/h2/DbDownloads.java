package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.SharedFile;

public class DbDownloads
{
	private static final QueryWrapper SELECT2 = new QueryWrapper("select * from PENDING_DOWNLOAD where Q_ID=?;");
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete from PENDING_DOWNLOAD where DSTATE=?;");
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from CHUNK join PENDING_DOWNLOAD on DID = Q_ID where DSTATE=?;");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select Q_ID from PENDING_DOWNLOAD where FID=?;");

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
			Services.logger.println("Unable to see if we want to download " + remoteFile);
			Services.logger.print(e);
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
			Services.logger.print(e);
		}
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE2))
		{
			stmt.setInt(1, DownloadState.ALL_DONE.toInt());
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
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
			Services.logger.println("Unable to get download " + parseInt);
			Services.logger.print(e);
		}
		return null;
	}
}
