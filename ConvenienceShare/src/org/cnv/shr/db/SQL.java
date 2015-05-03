package org.cnv.shr.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.db.SharedFileIterator.LocalFileIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public class SQL
{
	private static void addKey(Connection c, int machineId, String publicKey) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					 "insert or ignore into KEY(KEY, ADDED, MID) values(?, CURRENT_TIMESTAMP, ?);"))
		{
			int ndx = 1;
			stmt.setString(ndx++, publicKey);
			stmt.setInt   (ndx++, machineId);
			stmt.execute();
		}
	}
	
	static void addMachine(Connection c, Machine m) throws SQLException
	{
			try (PreparedStatement stmt = c.prepareStatement(
					DbConnection.SQLITE ?
					"insert or ignore into MACHINE(NAME, IP, PORT, LASTACTIVE, IDENT, LOCAL) values(?, ?, ?, CURRENT_TIMESTAMP, ?, ?);" :
						// need to check first...
					"merge into MACHINE(NAME, IP, PORT, LASTACTIVE, IDENT, LOCAL) values(?, ?, ?, CURRENT_TIMESTAMP, ?, ?);"		
					))
			{
				int ndx = 1;
				stmt.setString(ndx++, m.getName());
				stmt.setString(ndx++, m.getIp());
				stmt.setInt(   ndx++, m.getPort());
				stmt.setString(ndx++, m.getIdentifier());
				stmt.setInt(   ndx++, m.isLocal() ? 1 : 0);
				stmt.execute();
			}

			int machineId = getMachineId(c, m.getIp(), m.getPort());
			for (String publicKey : m.getKeys())
			{
				addKey(c, machineId, publicKey);
			}
	}

	static void addRoot(Connection c, Machine m, RootDirectory root) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"insert into ROOT(PATH, MID, LOCAL, TAGS, DESC)       " +
					"values (?, ?, ?, ?, ?);                              "))
		{
			int ndx = 1;
			stmt.setString(ndx++, root.getCanonicalPath());
			stmt.setInt	  (ndx++, m.getDbId());
			stmt.setInt   (ndx++, root.isLocal() ? 1 : 0);
			stmt.setString(ndx++, root.getTags());
			stmt.setString(ndx++, root.getDescription());
			stmt.execute();
		}
	}

	static void updateRoot(Connection c, Machine m, RootDirectory root) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"update ROOT                         " +
					"set NFILES=?,SPACE=?,TAGS=?,DESC=?  " +
					"where MID = ? and PATH=?;           "))
		{
			int ndx = 1;
			stmt.setLong  (ndx++, root.numFiles());
			stmt.setLong  (ndx++, root.diskSpace());
			stmt.setString(ndx++, root.getTags());
			stmt.setString(ndx++, root.getDescription());
			
			stmt.setInt   (ndx++, m.getDbId());
			stmt.setString(ndx++, root.getCanonicalPath());
			stmt.execute();
		}
	}
	static void deleteRoot(Connection c, Machine m, RootDirectory root) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"delete from ROOT           " +
					"where MID = ? and PATH=?;  "))
		{
			int ndx = 1;
			stmt.setLong  (ndx++, root.numFiles());
			stmt.setLong  (ndx++, root.diskSpace());
			stmt.setInt   (ndx++, m.getDbId());
			stmt.setString(ndx++, root.getCanonicalPath());
			stmt.execute();
		}
	}
	
	static List<LocalDirectory> getLocals(Connection c) throws SQLException
	{
		LinkedList<LocalDirectory> returnValue = new LinkedList<>();
		try (PreparedStatement stmt = c.prepareStatement(
					"select R_ID, PATH, TAGS, DESC, SPACE, NFILES from ROOT where ROOT.LOCAL = 1;"))
		{
			ResultSet executeQuery = stmt.executeQuery();
			while (executeQuery.next())
			{
				try
				{
					LocalDirectory local = new LocalDirectory(new File(executeQuery.getString(2)));
					local.setId           (executeQuery.getInt   (1));
					local.setPath         (executeQuery.getString(2));
					local.setTags         (executeQuery.getString(3));
					local.setDescription  (executeQuery.getString(4));
					local.setTotalFileSize(executeQuery.getLong  (5));
					local.setTotalNumFiles(executeQuery.getLong  (6));
					returnValue.add(local);
				}
				catch (IOException e)
				{
					Services.logger.logStream.println("Db contains path not in filesystem: " + executeQuery.getString(1));
					e.printStackTrace(Services.logger.logStream);
				}
			}
		}
		return returnValue;
	}
	
	static List<RemoteDirectory> listRemotes(Connection c, Machine machine) throws SQLException
	{
		LinkedList<RemoteDirectory> returnValue = new LinkedList<>();
		try (PreparedStatement stmt = c.prepareStatement(
					"select R_ID, PATH, TAGS, DESC, SPACE, NFILES from ROOT where MID = ?;"))
		{
			stmt.setInt(1, machine.getDbId());
			ResultSet executeQuery = stmt.executeQuery();
			while (executeQuery.next())
			{
				String path =         (executeQuery.getString(2));
				String tags =         (executeQuery.getString(3));
				String desc =         (executeQuery.getString(4));
				
				RemoteDirectory local = new RemoteDirectory(machine, path, tags, desc);

				local.setId           (executeQuery.getInt   (1));
				local.setTotalFileSize(executeQuery.getLong  (5));
				local.setTotalNumFiles(executeQuery.getLong  (6));

				returnValue.add(local);
			}
		}
		return returnValue;
	}
	static RootDirectory getRoot(Connection c, Machine machine, String canonicalPath) throws SQLException, IOException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select R_ID, PATH, TAGS, DESC, SPACE, NFILES from ROOT where ROOT.MID = ? and ROOT.PATH = ?;"))
		{
			stmt.setInt(1, machine.getDbId());
			stmt.setString(2, canonicalPath);
			ResultSet executeQuery = stmt.executeQuery();
			if (!executeQuery.next())
			{
				return null;
			}
			if (!executeQuery.getString(2).equals(canonicalPath))
			{
				throw new RuntimeException("This shouldn't happen...");
			}
			
			RootDirectory root = null;
			if (machine.isLocal())
			{
				root = new LocalDirectory(new File(executeQuery.getString(2)));
				root.setPath         (executeQuery.getString(2));
				root.setTags         (executeQuery.getString(3));
				root.setDescription  (executeQuery.getString(4));
			}
			else
			{
				root = new RemoteDirectory(machine, 
						executeQuery.getString(2),
						executeQuery.getString(3),
						executeQuery.getString(4));
			}
			root.setId           (executeQuery.getInt   (1));
			root.setTotalFileSize(executeQuery.getLong  (5));
			root.setTotalNumFiles(executeQuery.getLong  (6));
			return root;
		}
	}
	
	private static int getMachineId(Connection c, String ip, int port) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select M_ID                                " +
					"from MACHINE                               " +
					"where MACHINE.ip = ? and MACHINE.port = ?; "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, ip);
			stmt.setInt   (ndx++, port);
			ResultSet executeQuery = stmt.executeQuery();
			return executeQuery.getInt("M_ID");
		}
	}
	/*
	private static int getMachineId(Connection c, String publicKey) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select MID from KEY where KEY = ?;"))
		{
			int ndx = 1;
			stmt.setString(ndx++, publicKey);
			return stmt.executeQuery().getInt("MID");
		}
	}*/
//	private static int getMachineId(Connection c, String identifier) throws SQLException
//	{
//		try (PreparedStatement stmt = c.prepareStatement(
//					"select MID from KEY where IDENT = ?;"))
//		{
//			int ndx = 1;
//			stmt.setString(ndx++, identifier);
//			return stmt.executeQuery().getInt("MID");
//		}
//	}
//	
//	static Machine getMachine(Connection c, String ip, int port)
//	{
//		try (PreparedStatement stmt = c.prepareStatement(
//				"select PATH, TAGS from ROOT where ROOT.LOCAL = 1;"))
//	{
//		ResultSet executeQuery = stmt.executeQuery();
//		while (executeQuery.next())
//		{
//			try
//			{
//				returnValue.add(new LocalDirectory(new File(executeQuery.getString(1))));
//			}
//			catch (IOException e)
//			{
//				Services.logger.logStream.println("Db contains path not in filesystem: " + executeQuery.getString(1));
//				e.printStackTrace(Services.logger.logStream);
//			}
//		}
//	}
//	}

//	static Machine updateMachine(Connection c, Machine machine) throws SQLException
//	{
//		try (PreparedStatement stmt = c.prepareStatement(
//				"select M_ID, name, ip, port, lastactive, sharing, ident from MACHINE where ident = ?;"))
//		{
//			ResultSet resultSet = stmt.executeQuery();
//			if (!resultSet.next())
//			{
//				return null;
//			}
//			String ip = resultSet.getString("ip");
//			String name = resultSet.getString("name");
//			String identifier = resultSet.getString("ident");
//			int port = resultSet.getInt("port");
//			String[] keys = new String[0];
//
//			Machine machine = new Machine(ip, port, name, identifier, keys);
//
//			machine.setDbId(resultSet.getInt("M_ID"));
//			machine.setSharing(resultSet.getInt("sharing") == 1);
//			machine.setLastActive(resultSet.getLong("lastactive"));
//
//			return machine;
//		}
//	}
	static Machine getMachine(Connection c, String identifierKey) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				"select M_ID, name, ip, port, lastactive, sharing, ident, local from MACHINE where ident = ?;"))
		{
			stmt.setString(1, identifierKey);
			ResultSet resultSet = stmt.executeQuery();
			if (!resultSet.next())
			{
				return null;
			}
			if (resultSet.getInt("local") == 1)
			{
				Machine m = Services.localMachine;
				m.setDbId(resultSet.getInt("M_ID"));
				return m;
			}
			
			String ip = resultSet.getString("ip");
			String name = resultSet.getString("name");
			String identifier = resultSet.getString("ident");
			int port = resultSet.getInt("port");
			String[] keys = new String[0];

			Machine machine = new Machine(ip, port, name, identifier, keys);

			machine.setDbId(resultSet.getInt("M_ID"));
			machine.setSharing(resultSet.getInt("sharing") == 1);
			machine.setLastActive(resultSet.getLong("lastactive"));

			return machine;
		}
	}
	
	static List<Machine> getRemotes(Connection c) throws SQLException
	{
		LinkedList<Machine> returnValue = new LinkedList<>();
		try (PreparedStatement stmt = c.prepareStatement(
				"select M_ID, name, ip, port, lastactive, sharing, ident from MACHINE where LOCAL = 0"))
		{
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next())
			{
				String ip = resultSet.getString("ip");
				String name = resultSet.getString("name");
				String identifier = resultSet.getString("ident");
				int port = resultSet.getInt("port");
				String[] keys = new String[0];
				
				Machine machine = new Machine(ip, port, name, identifier, keys);

				machine.setDbId(resultSet.getInt("M_ID"));
				machine.setSharing(resultSet.getInt("sharing") == 1);
				machine.setLastActive(resultSet.getLong("lastactive"));

				returnValue.add(machine);
			}
		}
		return returnValue;
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
						"where PATH = ?                                             "))
		{
			int ndx = 1;
			stmt.setString(ndx++, file.getName()           );
			stmt.setLong  (ndx++, file.getFileSize()       );
			stmt.setInt   (ndx++, rootDirectoryId          );
			stmt.setString(ndx++, file.getChecksum()       );
			stmt.setLong  (ndx++, file.getLastUpdated()    );
			stmt.setString(ndx++, file.getRelativePath()  );
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
			stmt.setString(ndx++, file.getRelativePath() );
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
		ensurePath(c, f.getRelativePath());
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
		if (files.isEmpty())
		{
			return;
		}
		
		HashMap<String, Integer> pCache = new HashMap<>();
		
		StringBuilder sqlStmt = new StringBuilder();
		sqlStmt.append("insert into FILE(NAME, SIZE, PATH, ROOT, CHKSUM, MODIFIED) values ");
		Iterator<SharedFile> iterator = files.iterator();
		
		boolean first = true;
		while (iterator.hasNext())
		{
			SharedFile f = iterator.next();
			
			Integer pId = pCache.get(f.getRelativePath());
			if (pId == null)
			{
				pId = getPath(c, f.getRelativePath());
				pCache.put(f.getRelativePath(), pId);
			}

			if (first) { first = false; } else { sqlStmt.append(','); }
			
			sqlStmt.append("(")
				.append('\'').append(f.getName()).append("',")
				.append(f.getFileSize()).append(',')
				.append(pId).append(',')
				.append(directory.getId()).append(',')
				.append('\'').append(f.getChecksum()).append("',")
				.append(f.getLastUpdated()).append(')');
		}
		sqlStmt.append(";");
		
		try (PreparedStatement stmt = c.prepareStatement(sqlStmt.toString());)
		{
			stmt.execute();
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
	
	private static int getPath(Connection c, String path) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement("select P_ID from PATH where PATH.PATH=?;"))
		{
			stmt.setString(1, path);
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getInt(1);
			}
		}
		
		ensurePath(c, path);
		try (PreparedStatement stmt = c.prepareStatement("select P_ID from PATH where PATH.PATH=?;"))
		{
			stmt.setString(1, path);
			ResultSet executeQuery = stmt.executeQuery();
			if (executeQuery.next())
			{
				return executeQuery.getInt(1);
			}
			else
			{
				return -1;
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
