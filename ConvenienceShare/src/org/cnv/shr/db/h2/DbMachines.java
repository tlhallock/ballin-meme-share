package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;

public class DbMachines
{
	public static void addMachine(Machine m) throws SQLException
	{
		Connection c = Services.h2DbCache.getConnection();

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
