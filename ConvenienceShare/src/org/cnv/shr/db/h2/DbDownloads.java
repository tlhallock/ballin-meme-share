package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.SharedFile;

public class DbDownloads
{
	public static boolean hasPendingDownload(SharedFile remoteFile)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select Q_ID from PENDING_DOWNLOAD where FID=?;"))
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
        
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("delete from CHUNK join PENDING_DOWNLOAD on DID = Q_ID where DSTATE=?;"))
		{
			stmt.setInt(1, DownloadState.ALL_DONE.toInt());
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		try (PreparedStatement stmt = c.prepareStatement("delete from PENDING_DOWNLOAD where DSTATE=?;"))
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
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select * from PENDING_DOWNLOAD where Q_ID=?;"))
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
