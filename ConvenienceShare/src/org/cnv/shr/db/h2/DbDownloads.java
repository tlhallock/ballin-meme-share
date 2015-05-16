package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
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
}
