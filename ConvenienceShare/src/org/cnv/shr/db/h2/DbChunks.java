
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.util.LogWrapper;

public class DbChunks
{
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from CHUNK where DID=?;");
	private static final QueryWrapper SELECT4 = new QueryWrapper("select END_OFFSET, BEGIN_OFFSET from CHUNK where DID=? and DOWNLOAD_STATE=" + ChunkState.DOWNLOADED.getDbValue() + ";");
	private static final QueryWrapper UPDATE1 = new QueryWrapper("update CHUNK set DOWNLOAD_STATE=? where DID=? and END_OFFSET=? and BEGIN_OFFSET=? and CHECKSUM=?;");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select END_OFFSET, BEGIN_OFFSET, CHECKSUM from CHUNK where DID=? and DOWNLOAD_STATE=" + ChunkState.NOT_DOWNLOADED.getDbValue() + " limit ?;");
	private static final QueryWrapper SELECT5 = new QueryWrapper("select count(C_ID) from CHUNK where DID=? and END_OFFSET=? and BEGIN_OFFSET=?;");
	private static final QueryWrapper COUNT   = new QueryWrapper("select count(C_ID) from CHUNK where DID=?;");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select * from CHUNK where DID=?;");
	private static final QueryWrapper MERGE1  = new QueryWrapper("merge into CHUNK key(DID, BEGIN_OFFSET, END_OFFSET) values (DEFAULT, ?, ?, ?, ?, ?);");
	private static final QueryWrapper SELECT6 = new QueryWrapper("select count(C_ID) from CHUNK where DID=? and END_OFFSET=? and BEGIN_OFFSET=? and DOWNLOAD_STATE=" + ChunkState.DOWNLOADED.getDbValue() + ";");

	public static final class DbChunk extends DbObject<Integer>
	{
		public DbChunk(Integer id)
		{
			super(id);
		}
		public Chunk chunk;
		public ChunkState state;
		
		@Override
		public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
		{
			int ndx = 1;
			int id = row.getInt(ndx++);
			int downloadId = row.getInt(ndx++);
			long begin = row.getLong(ndx++);
			long end = row.getLong(ndx++);
			String checksum = row.getString(ndx++);
			state = ChunkState.getState(row.getInt(ndx++));
			chunk = new Chunk(begin, end, checksum);
		}
		@Override
		public boolean save(ConnectionWrapper c) throws SQLException
		{
			throw new RuntimeException("This should be moved to its own class...");
		}
	}
	
	public static DbIterator<DbChunk> getAllChunks(Download d) throws SQLException
	{
		ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
		StatementWrapper stmt = c.prepareStatement(SELECT1);
		stmt.setInt(1, d.getId());
		return new DbIterator<DbChunks.DbChunk>(c, stmt.executeQuery(), DbObjects.CHUNK);
	}

	public static void addChunk(Download d, Chunk c)
	{
		try (ConnectionWrapper con = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = con.prepareStatement(MERGE1, PreparedStatement.RETURN_GENERATED_KEYS);)
		{
			int ndx = 1;
			// the actual values...
			stmt.setInt(ndx++, d.getId());
			stmt.setLong(ndx++, c.getBegin());
			stmt.setLong(ndx++, c.getEnd());
			stmt.setString(ndx++, c.getChecksum());
			stmt.setInt(ndx++, ChunkState.NOT_DOWNLOADED.getDbValue());
			
			stmt.executeUpdate();
			
			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					int id = generatedKeys.getInt(1);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to add chunk " + c + " to " + d, e);
		}
	}

	public static boolean hasAllChunks(Download d)
	{
		long chunkSize = d.getChunkSize();
		// should check for each chunk, in reverse order...
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT5))
		{
			long fileSize = d.getFile().getFileSize();
			long end   = fileSize; 
			long start = fileSize - fileSize % chunkSize;
			while (end > 0)
			{
				stmt.setInt(1, d.getId());
				stmt.setLong(2, end);
				stmt.setLong(3, start);
				try (ResultSet results = stmt.executeQuery();)
				{
					if (!results.next() || results.getInt(1) <= 0)
					{
						LogWrapper.getLogger().info("Download " + d + " is missing chunk " + start + "-" + end);
						return false;
					}
				}
				
				end = start;
				start = start - chunkSize;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to see if we have all chunks for " + d, e);
			return false;
		}
		LogWrapper.getLogger().info("All chunks done for " + d);
		return true;
	}

	public static boolean allChunksAreDone(Download d)
	{
		long chunkSize = d.getChunkSize();
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT6))
		{
			long fileSize = d.getFile().getFileSize();
			long end   = fileSize; 
			long start = fileSize - fileSize % chunkSize;
			while (end > 0)
			{
				stmt.setInt(1, d.getId());
				stmt.setLong(2, end);
				stmt.setLong(3, start);
				try (ResultSet results = stmt.executeQuery();)
				{
					if (!results.next() || results.getInt(1) <= 0)
					{
						LogWrapper.getLogger().info("Download " + d + " has not completed chunk " + start + "-" + end);
						return false;
					}
				}
				
				end = start;
				start = start - chunkSize;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to see if all chunks are downloaded for " + d, e);
			return false;
		}
		LogWrapper.getLogger().info("All chunks downloaded for " + d);
		return true;
	}

	public static List<Chunk> getNextChunks(Download d, int max)
	{
		List<Chunk> returnValue = new LinkedList<>();
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT3))
		{
			stmt.setInt(1, d.getId()); // this file
			stmt.setInt(2, max);
			try (ResultSet results = stmt.executeQuery();)
			{
				while (results.next() && returnValue.size() < max)
				{
					long end = results.getLong(1);
					long begin = results.getLong(2);
					String checksum = results.getString(3);
					returnValue.add(new Chunk(begin, end, checksum));
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get the next chunks.", e);
		}
		return returnValue;
	}
	
	public static void chunkDone(Download d, Chunk chunk, ChunkState done)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(UPDATE1))
		{
			int ndx = 1;
			stmt.setInt(ndx++, done.getDbValue());
			stmt.setInt(ndx++, d.getId());
			stmt.setLong(ndx++, chunk.getEnd());
			stmt.setLong(ndx++, chunk.getBegin());
			stmt.setString(ndx++, chunk.getChecksum());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to set chunk status to done.", e);
		}
	}

	public static void removeAllChunks(Download d)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1);)
		{
			stmt.setInt(1, d.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove chunks", e);
		}
	}
	
	public static double getDownloadPercentage(Download d)
	{
		long total = 0;
		long numDone = 0;
		long doneBytes = 0;
		
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				 StatementWrapper stmt = c.prepareStatement(SELECT4);
				 StatementWrapper count = c.prepareStatement(COUNT);)
		{
			count.setInt(1, d.getId()); // this file
			try (ResultSet results = count.executeQuery();)
			{
				if (results.next())
				{
					total = results.getLong(1);
				}
			}
			
			stmt.setInt(1, d.getId()); // this file

			try (ResultSet results = stmt.executeQuery();)
			{
				while (results.next())
				{
					numDone++;
					doneBytes += results.getLong(1) - results.getLong(2);
				}
			}
			
			LogWrapper.getLogger().info(numDone + " chunks done out of " + total + " for download " + d);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get download percentage", e);
		}
		return doneBytes / (double) d.getFile().getFileSize();
	}
	
	
	public enum ChunkState
	{
		NOT_DOWNLOADED(1),
		REQUESTED(2),
		DOWNLOADED(3),
		;
		int state;
		
		ChunkState(int dbValue)
		{
			this.state = dbValue;
		}
		
		public int getDbValue()
		{
			return state;
		}

		static ChunkState getState(int value)
		{
			for (ChunkState v : values())
			{
				if (v.state == value)
				{
					return v;
				}
			}
			return NOT_DOWNLOADED;
		}

		public boolean isDone()
		{
			return equals(DOWNLOADED);
		}
	}
}


//public static double getDownloadPercentage(Download d)
//{
//try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
//		StatementWrapper stmt = c.prepareStatement(SELECT4);)
//{
//	int ndx = 1;
//	// the query
//	stmt.setInt(ndx++, d.getId()); // this file
//
//	try (ResultSet results = stmt.executeQuery();)
//	{
//		if (results.next())
//		{
//			return results.getLong(1) / (double) d.getFile().getFileSize();
//		}
//	}
//}
//catch (SQLException e)
//{
//	LogWrapper.getLogger().log(Level.INFO, "Unable to get download percentage", e);
//}
//return 0.0;
//}
//private static final QueryWrapper SELECT4 = new QueryWrapper("select SUM(END_OFFSET-BEGIN_OFFSET) from CHUNK where DID=? and IS_DOWNLOADED=true;");
