package org.cnv.shr.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.cnv.shr.db.SharedFileIterator.LocalFileIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public class Files
{
	private static void ensurePath(Connection c, String path) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"insert or ignore into PATH (PATH) values (?);"))
		{
			int ndx = 1;
			stmt.setString(ndx++, path);
			stmt.execute();
		}
	}
	
	static int getRootDirectoryId(Connection c, Machine m, String path) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select R_ID                                             " +
					"from ROOT join MACHINE on M_ID = MID                    " +
					"where PATH = ? and MACHINE.ip = ? and MACHINE.port = ?; "))
		{
			int ndx = 1;
			stmt.setString(ndx++, path);
			stmt.setString(ndx++, m.getIp());
			stmt.setInt   (ndx++, m.getPort());
			
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getInt("R_ID");
			}
			return -1;
		}
	}
	
	static long getTotalFileSize(Connection c, RootDirectory d) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"select sum(SIZE) as totalsize from FILE where ROOT = ?;"))
		{
			stmt.setInt(1, d.getId());
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getLong("totalsize");
			}
			else
			{
				return -1;
			}
		}
	}
	
	static long getNumberOfFiles(Connection c, RootDirectory d) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"select count(F_ID) as number from FILE where ROOT = ?;"))
		{
			stmt.setInt(1, d.getId());
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getLong("number");
			}
			else
			{
				return -1;
			}
		}
	}

	private static void addFileWithChecksum(Connection c, int rootDirectoryId, SharedFile file) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"insert into FILE(NAME, SIZE, PATH, ROOT, CHKSUM, MODIFIED) " +
						"select ?, ?, P_ID, ?, ?, ?                                 " +
						"from PATH                                                  " +
						"where PATH = ?                                             "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, file.getName()           );
			stmt.setLong  (ndx++, file.getFileSize()       );
			stmt.setInt   (ndx++, rootDirectoryId          );
			stmt.setString(ndx++, file.getChecksum()       );
			stmt.setLong  (ndx++, file.getLastUpdated()    );
			stmt.setString(ndx++, file.getCanonicalPath()  );
			stmt.execute();
		}
	}
	
	private static void addFileNoChecksum(Connection c, int rootDirectoryId, SharedFile file) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"insert into FILE(NAME, SIZE, ROOT, PATH, MODIFIED) " +
						"select ?, ?, ?, P_ID, ?                            " +
						"from PATH                                          " +
						"where PATH = ?;                                    "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, file.getName() );
			stmt.setLong  (ndx++, file.getFileSize() );
			stmt.setInt   (ndx++, rootDirectoryId);
			stmt.setLong  (ndx++, file.getLastUpdated()    );
			stmt.setString(ndx++, file.getCanonicalPath() );
			stmt.execute();
		}
	}
	
	static void removeFile(Connection c, int fileId) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement("delete from FILE where FILE.F_ID = ?;"))
		{
			stmt.setInt(1, fileId);
			stmt.execute();
		}
	}
	private static void addFile(Connection c, int rootId, SharedFile f) throws SQLException
	{
		ensurePath(c, f.getCanonicalPath());
		if (f.getChecksum() == null)
		{
			addFileNoChecksum(c, rootId, f);
		}
		else
		{
			addFileWithChecksum(c, rootId, f);
		}
	}
	
	private static void updateFileWithChecksum(Connection c, int rootDirectoryId, SharedFile file) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"update FILE                                  " +
						"set SIZE=?, CHKSUM=?,STATE=?,MODIFIED=?      " +
						"where F_ID = ?                               "))
		{
			int ndx = 1;
			stmt.setLong  (ndx++, file.getFileSize()       );
			stmt.setString(ndx++, file.getChecksum()       );
			stmt.setInt   (ndx++, 0                        );
			stmt.setLong  (ndx++, file.getLastUpdated()    );
			stmt.setInt   (ndx++, file.getId()             );
			stmt.execute();
		}
	}
	
	private static void updateFileNoChecksum(Connection c, int rootDirectoryId, SharedFile file) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"update FILE                        " +
						"set SIZE=?,STATE=?,MODIFIED=?      " +
						"where F_ID = ?                     "))
		{
			int ndx = 1;
			stmt.setLong  (ndx++, file.getFileSize()       );
			stmt.setInt   (ndx++, 0                        );
			stmt.setLong  (ndx++, file.getLastUpdated()    );
			stmt.setInt   (ndx++, file.getId()             );
			stmt.execute();
		}
	}
	static void updateFile(Connection c, int rootId, SharedFile f) throws SQLException
	{
		if (f.getChecksum() == null)
		{
			updateFileNoChecksum(c, rootId, f);
		}
		else
		{
			updateFileWithChecksum(c, rootId, f);
		}
	}
	
	static void addFiles(Connection c, RootDirectory directory, List<SharedFile> files) throws SQLException
	{
		int rootId = directory.getId();
		if (rootId < 0)
		{
			Services.logger.logStream.println("Unable to find root id for " + directory.getCanonicalPath());
			return;
		}
		for (SharedFile file : files)
		{
			addFile(c, rootId, file);
		}
	}
	
	static void addFile(Connection c, RootDirectory directory, SharedFile file) throws SQLException
	{
		int rootId = directory.getId();
		if (rootId < 0)
		{
			Services.logger.logStream.println("Unable to find root id for " + directory.getCanonicalPath());
			return;
		}
		addFile(c, rootId, file);
	}

	public static LocalFile getFile(Connection c, RootDirectory directory, String relPath, String name) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				"select * from FILE                                       " +
				"join PATH on FILE.PATH = PATH.P_ID                       " +
				" where FILE.ROOT = ? and FILE.NAME = ? and PATH.PATH = ?;");)
		{
			int ndx = 1;
			stmt.setInt(ndx++, directory.getId());
			stmt.setString(ndx++, name);
			stmt.setString(ndx++, relPath);
			
			LocalFileIterator localFileIterator = new SharedFileIterator.LocalFileIterator(  (LocalDirectory) directory, stmt.executeQuery());
			if (localFileIterator.hasNext())
			{
				return (LocalFile) localFileIterator.next();
			}
			else
			{
				return null;
			}
		}
	}
	
	static String getPath(Connection c, int pathId) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select PATH from PATH where P_ID = ?;"))
		{
			stmt.setInt(1, pathId);
			
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getString("PATH");
			}
			return "unknown";
		}
	}

	public static Iterator<SharedFile> list(Connection c, RootDirectory d) throws SQLException
	{
		PreparedStatement stmt = c.prepareStatement("select F_ID, NAME, SIZE, CHKSUM, PATH, ROOT, STATE, MODIFIED from FILE where FILE.ROOT = ?;");
		stmt.setInt(1, d.getId());
		if (d.isLocal())
		{
			return new SharedFileIterator.LocalFileIterator(  (LocalDirectory) d, stmt.executeQuery());
		}
		else
		{
			return new SharedFileIterator.RemoteFileIterator((RemoteDirectory) d, stmt.executeQuery());
		}
	}
	
	static void removeUnusedPaths(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				"delete from PATH                 " + 
				"where P_ID not in                " + 
				"(		                          " + 
			    "        select distinct PATH     " + 
			    "        from FILE                " +
			    ");                               ");)
		{
			stmt.execute();
		}
	}
}
