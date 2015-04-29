package org.cnv.shr.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
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
					"select MID from KEY where KEY = ?;"
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, publicKey);
			return stmt.executeQuery().getInt("MID");
		}
	}
	
	static void addMachine(Machine m) throws SQLException
	{
		try (Connection c = Services.db.getConnection();)
		{
			try (PreparedStatement stmt = c.prepareStatement(
					"insert or ignore into MACHINE(NAME, IP, PORT, LASTACTIVE) values(?, ?, ?, CURRENT_TIMESTAMP);"))
			{
				int ndx = 1;
				stmt.setString(ndx++, m.getName());
				stmt.setString(ndx++, m.getIp());
				stmt.setInt(ndx++, m.getPort());
				stmt.execute();

			}

			int machineId = getMachineId(c, m.getIp(), m.getPort());
			for (String publicKey : m.getKeys())
			{
				addKey(c, machineId, publicKey);
			}
		}
	}

	static void addRoot(Machine m, RootDirectory root) throws SQLException
	{
		try (Connection c = Services.db.getConnection();
			 PreparedStatement stmt = c.prepareStatement(
					"insert into ROOT(PATH, MID)                          " +
					"select ?, M_ID                                       " +
					"from MACHINE where MACHINE.ip=? and MACHINE.port=?;  "
				))
		{
			int ndx = 1;
			stmt.setString(ndx++, root.getPath());
			stmt.setString(ndx++, m.getIp()  );
			stmt.setInt   (ndx++, m.getPort());
			stmt.execute();
		}
	}
	
	static Machine getMachine(String ip, int port)
	{
		return null;
	}
}
