package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.mdl.Download;

public class DbChunks
{
	public static final class DbChunk extends DbObject<Integer>
	{
		public DbChunk(Integer id)
		{
			super(id);
		}
		public Chunk chunk;
		public boolean done;
		
		@Override
		public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
		{
			int ndx = 1;
			long begin = row.getLong(ndx++);
			long end = row.getLong(ndx++);
			String checksum = row.getString(ndx++);
			chunk = new Chunk(begin, end, checksum);
			done = row.getBoolean(ndx++);
		}
		@Override
		public boolean save(Connection c) throws SQLException
		{
			throw new RuntimeException("This should be moved to its own class...");
		}
	}
	
	public static DbIterator<DbChunk> getAllChunks(Download d) throws SQLException
	{
		Connection c = Services.h2DbCache.getConnection();
		PreparedStatement stmt = c.prepareStatement(
				 "select END_OFFSET, BEGIN_OFFSET, CHECKSUM from CHUNK where DID=?;");
		stmt.setInt(1, d.getId());
		return new DbIterator<DbChunks.DbChunk>(c, stmt.executeQuery(), DbObjects.CHUNK);
	}

	public static void addChunk(Download d, Chunk c)
	{
//		@Override
//		public boolean save(Connection c) throws SQLException
//		{
//			try (PreparedStatement stmt = c.prepareStatement(
//					 "merge into CHUNK key(DID, BEGIN_OFFSET, END_OFFSET) values "
//					 + "((select C_ID from CHUNK where DID=?, BEGIN_OFFSET=?, END_OFFSET=?),"
//					 + " ?, ?, ?, ?, ?);"
//					, Statement.RETURN_GENERATED_KEYS);)
//			{
//				int ndx = 1;
//				// the query
//				stmt.setInt(ndx++, x); // ths file
//				stmt.setLong(ndx++, begin);
//				stmt.setLong(ndx++, end);
//				
//				// the actual values...
//				stmt.setInt(ndx++, x); // the file
//				stmt.setLong(ndx++, begin);
//				stmt.setLong(ndx++, end);
//				stmt.setString(ndx++, checksum);
//              stmt.setBoolean(ndx++, false);		
//				stmt.executeUpdate();
//				ResultSet generatedKeys = stmt.getGeneratedKeys();
//				if (generatedKeys.next())
//				{
//					id = generatedKeys.getInt(1);
//				}
//			}
//			return true;
//		}		
	}

	public static boolean hasAllChunks(Download d, long chunkSize)
	{
		int count = 0;
		for (long l = 0; l < d.getFile().getFileSize(); l += chunkSize)
		{
			count++;
		}
		
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				 "select count(C_ID) from CHUNK where DID=?;"))
		{
			stmt.setInt(1, d.getId());
			ResultSet results = stmt.executeQuery();
			if (!results.next())
			{
				return false;
			}
			return results.getInt(1) == count;
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		return false;
	}
	public static List<Chunk> getNextChunks(Download d, int max)
	{
		List<Chunk> returnValue = new LinkedList<>();
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				 "select END_OFFSET, BEGIN_OFFSET, CHECKSUM from CHUNK where DID=? and IS_DOWNLOADED=false;"))
		{
			stmt.setInt(1, d.getId()); // ths file
			ResultSet results = stmt.executeQuery();
			while (results.next() && returnValue.size() < max)
			{
				long end = results.getLong(1);
				long begin = results.getLong(2);
				String checksum = results.getString(3);
				returnValue.add(new Chunk(begin, end, checksum));
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		return returnValue;
	}
	
	public static void chunkDone(Download d, Chunk chunk, boolean done)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				 "update CHUNK set IS_DOWNLOADED=? where DID=? and END_OFFSET=? and BEGIN_OFFSET=? and CHECKSUM=?;"))
		{
			int ndx = 1;
			stmt.setBoolean(ndx++, done);
			stmt.setInt(ndx++, d.getId());
			stmt.setLong(ndx++, chunk.getBegin());
			stmt.setLong(ndx++, chunk.getEnd());
			stmt.setString(ndx++, chunk.getChecksum());
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}

	public static void allChunksDone(Download d)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				 "delete from CHUNK where DID=?;"))
		{
			stmt.setInt(1, d.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
	
	public static double getDownloadPercentage(Download d)
	{
		long done = 0;
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				 "select END_OFFSET, BEGIN_OFFSET, END_OFFSET-BEGIN_OFFSET from CHUNK where DID=? and IS_DOWNLOADED=true;"))
		{
			int ndx = 1;
			// the query
			stmt.setInt(ndx++, d.getId()); // ths file
			
			ResultSet results = stmt.executeQuery();
			while (results.next())
			{
				done += results.getLong(1) - results.getLong(2);
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		return done / (double) d.getFile().getFileSize();
	}
}
