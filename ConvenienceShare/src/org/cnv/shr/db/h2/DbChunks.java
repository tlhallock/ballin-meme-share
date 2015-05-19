package org.cnv.shr.db.h2;

import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.mdl.Download;

public class DbChunks
{

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
	
	public static Chunk getNextChunk(Download d)
	{
		return null;
	}
	
	public static void done(Chunk c)
	{
		
	}
}
