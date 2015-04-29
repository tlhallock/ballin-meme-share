package org.cnv.shr.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public class Files
{
	private static void ensurePath(Connection c, String path) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"insert or ignore into PATH (PATH) values (?);"
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, path);
			stmt.execute();
		}
	}
	
	private static int getRootDirectoryId(Connection c, Machine m, String path) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select R_ID                                             " +
					"from ROOT join MACHINE on M_ID = MID                    " +
					"where PATH = ? and MACHINE.ip = ? and MACHINE.port = ?; "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, path);
			stmt.setString(ndx++, m.getIp());
			stmt.setInt   (ndx++, m.getPort());
			ResultSet executeQuery = stmt.executeQuery();
			return executeQuery.getInt("R_ID");
		}
	}

	private static void addFileWithChecksum(Connection c, int rootDirectoryId, SharedFile file) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"insert into FILE(NAME, SIZE, PATH, ROOT, CHKSUM) " +
						"select ?, ?, P_ID, ?, ?                          " +
						"from PATH                                        " +
						"where PATH = ?                                   "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, file.getName()    );
			stmt.setLong  (ndx++, file.getSize()    );
			stmt.setInt   (ndx++, rootDirectoryId   );
			stmt.setString(ndx++, file.getChecksum());
			stmt.setString(ndx++, file.getPath()    );
			stmt.execute();
		}
	}
	
	private static void addFileNoChecksum(Connection c, int rootDirectoryId, SharedFile file) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
						"insert into FILE(NAME, SIZE, ROOT, PATH) " +
						"select ?, ?, ?, P_ID                     " +
						"from PATH                                " +
						"where PATH = ?;                          "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, file.getName() );
			stmt.setLong  (ndx++, file.getSize() );
			stmt.setInt   (ndx++, rootDirectoryId);
			stmt.setString(ndx++, file.getPath() );
			stmt.execute();
		}
	}
	
	private static void updateFile(Connection c, int rootId, SharedFile f) throws SQLException
	{
		ensurePath(c, f.getPath());
		if (f.getChecksum() == null)
		{
			addFileNoChecksum(c, rootId, f);
		}
		else
		{
			addFileWithChecksum(c, rootId, f);
		}
	}

	static void addFiles(Connection c, RootDirectory directory, List<SharedFile> files) throws SQLException
	{
		int rootId = getRootDirectoryId(c, directory.getMachine(), directory.getPath());
		for (SharedFile file : files)
		{
			updateFile(c, rootId, file);
		}
	}
}
