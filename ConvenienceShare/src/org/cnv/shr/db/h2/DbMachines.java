package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.cnv.shr.mdl.Machine;

public class DbMachines
{
	public static void addMachine(Connection c, Machine m) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				"merge into MACHINE(NAME, IP, PORT, LASTACTIVE, IDENT, LOCAL) values(?, ?, ?, CURRENT_TIMESTAMP, ?, ?);"))
		{
			int ndx = 1;
			stmt.setString(ndx++, m.getName());
			stmt.setString(ndx++, m.getIp());
			stmt.setInt(   ndx++, m.getPort());
			stmt.setString(ndx++, m.getIdentifier());
			stmt.setInt(   ndx++, m.isLocal() ? 1 : 0);
			stmt.execute();
		}

//		int machineId = getMachineId(c, m.getIp(), m.getPort());
//		for (String publicKey : m.getKeys())
//		{
//			addKey(c, machineId, publicKey);
//		}
	}
	
	public static void updateMachine(Connection c, Machine machine) throws SQLException
	{
		
	}
	
	public static void removeMachine(Connection c, Machine machine) throws SQLException
	{
		
	}
	
	public DbIterator<Machine> listRemoteMachines(Connection c) throws SQLException
	{
		return null;
	}
	
	public static Machine getMachine(Connection c, String identifier) throws SQLException
	{
		return null;
	}
	public static Machine getMachine(Connection c, int machineId) throws SQLException
	{
		return null;
	}
}
