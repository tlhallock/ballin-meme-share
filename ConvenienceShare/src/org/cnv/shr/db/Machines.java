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
	
	private static int getMachineId(Connection c, String publicKey) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
					"select MID from KEY where KEY = ?;"))
		{
			int ndx = 1;
			stmt.setString(ndx++, publicKey);
			return stmt.executeQuery().getInt("MID");
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
					"insert into ROOT(PATH, MID, LOCAL)                   " +
					"select ?, M_ID, ?                                    " +
					"from MACHINE where MACHINE.ip=? and MACHINE.port=?;  "))
		{
			int ndx = 1;
			stmt.setString(ndx++, root.getCanonicalPath());
			stmt.setInt   (ndx++, root.isLocal() ? 1 : 0);
			stmt.setString(ndx++, m.getIp()  );
			stmt.setInt   (ndx++, m.getPort());
			stmt.execute();
		}
	}
	
	static List<LocalDirectory> getLocals(Connection c) throws SQLException
	{
		LinkedList<LocalDirectory> returnValue = new LinkedList<>();
		try (PreparedStatement stmt = c.prepareStatement(
					"select PATH, TAGS from ROOT where ROOT.LOCAL = 1;"))
		{
			ResultSet executeQuery = stmt.executeQuery();
			while (executeQuery.next())
			{
				try
				{
					returnValue.add(new LocalDirectory(new File(executeQuery.getString(1))));
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
	
	static Machine getMachine(Connection c, String ip, int port)
	{
		return null;
	}
	
	static List<Machine> getRemotes(Connection c) throws SQLException
	{
		LinkedList<Machine> returnValue = new LinkedList<>();
		try (PreparedStatement stmt = c.prepareStatement(
				"select name, ip, port, lastactive, sharing from MACHINE where LOCAL = 0"))
		{
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next())
			{
				Machine machine = new Machine(resultSet.getString("ip"), resultSet.getInt("port"));
				
				machine.setSharing(resultSet.getInt("sharing") == 1);
				machine.setName(resultSet.getString("name"));
				machine.setLastActive(resultSet.getLong("lastactive"));
				
				returnValue.add(machine);
			}
		}
		return returnValue;
	}
}
