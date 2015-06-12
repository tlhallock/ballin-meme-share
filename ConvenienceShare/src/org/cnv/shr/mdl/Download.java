
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


package org.cnv.shr.mdl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public class Download extends DbObject<Integer>
{
	private static final QueryWrapper MERGE1  = new QueryWrapper("merge into DOWNLOAD key(FID) values ((select Q_ID from DOWNLOAD where FID=?), ?, ?, ?, ?, ?)");
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from DOWNLOAD where FID=?");
	private static final QueryWrapper UPDATE1 = new QueryWrapper("update DOWNLOAD set DSTATE=? where Q_ID=?");
	
	private RemoteFile file;
	private DownloadState currentState;
	private long added;
	private int priority;
	private Path destinationFile;
	private int chunkSize = -1;
	
	public Download(RemoteFile remote)
	{
		super(DbDownloads.getPendingDownloadId(remote));
		this.file = remote;
		setChunkSize();
		this.currentState = DownloadState.QUEUED;
		this.added = System.currentTimeMillis();
	}

	public Download(int int1)
	{
		super(int1);
	}
	
	public RemoteFile getFile()
	{
		return file;
	}
	
	private void setChunkSize()
	{
		chunkSize = 1024 * 1024;
		int better = (int) Math.min(50 * 1024 * 1024, file.getFileSize() / 100);
		if (better > chunkSize)
		{
			chunkSize = better;
		}
	}
	
	public long getChunkSize()
	{
		if (chunkSize < 0 && file != null)
		{
			setChunkSize();
		}
		return chunkSize;
	}

	@Override
	public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
	{
		int ndx = 1;
		this.id = row.getInt(ndx++);
		this.file = (RemoteFile) DbFiles.getFile(row.getInt(ndx++));
		this.added = row.getLong(ndx++);
		this.currentState = DownloadState.getState(row.getInt(ndx++));
		this.priority = row.getInt(ndx++);
		this.destinationFile = Paths.get(row.getString(ndx++));
	}
	
	public void setState(DownloadState state)
	{
		this.currentState = state;
		LogWrapper.getLogger().info("Setting download state for " + this + " to " + state.name());
		
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(UPDATE1);)
		{
			stmt.setInt(1, state.dbValue);
			stmt.setInt(2, id);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save download state", e);
		}
	}

	public long getAdded()
	{
		return added;
	}
	
	public int getPriority()
	{
		return priority;
	}
	
	public void delete()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1);)
		{
			stmt.setInt(1, file.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete", e);
		}
	}

	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		try (StatementWrapper stmt = c.prepareStatement(MERGE1);)
		{
			int ndx = 1;
			stmt.setInt(ndx++, file.getId());
			stmt.setInt(ndx++, file.getId());
			stmt.setLong(ndx++, added);
			stmt.setInt(ndx++, currentState.dbValue);
			stmt.setInt(ndx++, priority);
			stmt.setString(ndx++, getTargetFile().toString());
			stmt.executeUpdate();
			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
					return true;
				}
				return false;
			}
		}
	}
	
	@Override
	public String toString()
	{
		return "Download of " + file;
	}

	public DownloadState getState()
	{
		return currentState;
	}

	public Path getTargetFile()
	{
		// Should check if currently downloading?
		if (destinationFile != null)
		{
			return destinationFile.normalize();
		}
		return file.getTargetFile().normalize();
	}
	
	public enum DownloadState
	{
		QUEUED                       ( 1),
//		NOT_STARTED                   (2),
		ALLOCATING                   ( 3),
		RECOVERING                   ( 4),
		REQUESTING_METADATA          ( 5),
		RECEIVING_METADATA           ( 6),
//		FINDING_PEERS                 (6),
		DOWNLOADING                  ( 7),
		VERIFYING_COMPLETED_DOWNLOAD ( 8),
		PLACING_IN_FS                ( 9),
		ALL_DONE                     (10),
		
		;

		int dbValue;

		DownloadState(int value)
		{
			this.dbValue = value;
		}

		public int toInt()
		{
			return dbValue;
		}

		static DownloadState getState(int dbValue)
		{
			for (DownloadState s : DownloadState.values())
			{
				if (s.dbValue == dbValue)
				{
					return s;
				}
			}
			LogWrapper.getLogger().info("Unknown file state: " + dbValue);
			return null;
		}

		public String humanReadable()
		{
			return name();
		}
		
		public boolean comesAfter(DownloadState other)
		{
			return dbValue > other.dbValue;
		}
		public boolean hasYetTo(DownloadState other)
		{
			return !comesAfter(other);
		}
	}
	
	public enum SharedFileState
	{
		LOCAL          (0),
		REMOTE         (1),
		QUEUED         (2),
		DOWNLOADING    (3),
		DOWNLOADED     (4),
		HAVE_COPY      (5),
		
		;
		
		int dbValue;
		
		SharedFileState(int value)
		{
			dbValue = value;
		}
		
		public int toInt()
		{
			return dbValue;
		}
		
		static SharedFileState getState(int dbValue)
		{
			for (SharedFileState s : SharedFileState.values())
			{
				if (s.dbValue == dbValue)
				{
					return s;
				}
			}
			LogWrapper.getLogger().info("Unknown file state: " + dbValue);
			return null;
		}
	}

	public void setDestination(Path destinationFile2)
	{
		destinationFile = destinationFile2;
		tryToSave();
	}
}
