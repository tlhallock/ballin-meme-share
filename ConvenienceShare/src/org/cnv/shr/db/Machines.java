package org.cnv.shr.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;

public class Machines
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
					"insert or ignore into MACHINE(NAME, IP, PORT, LASTACTIVE, IDENT, LOCAL) values(?, ?, ?, CURRENT_TIMESTAMP, ?, ?);"))
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
	
	static List<RemoteDirectory> getRoots(Connection c, Machine machine) throws SQLException
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
	static RootDirectory getRoot(Connection c, Machine machine, String canonicalPath) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select PATH, TAGS, DESC from ROOT where MID = ? and PATH = ?;"))
		{
			stmt.setInt(1, machine.getDbId());
			stmt.setString(2, canonicalPath);
			ResultSet executeQuery = stmt.executeQuery();
			if (!executeQuery.next())
			{
				return null;
			}
			return new RemoteDirectory(machine, 
					executeQuery.getString(1),
					executeQuery.getString(2),
					executeQuery.getString(3));
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
				"select M_ID, name, ip, port, lastactive, sharing, ident from MACHINE where ident = ?;"))
		{
			stmt.setString(1, identifierKey);
			ResultSet resultSet = stmt.executeQuery();
			if (!resultSet.next())
			{
				return null;
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
}
