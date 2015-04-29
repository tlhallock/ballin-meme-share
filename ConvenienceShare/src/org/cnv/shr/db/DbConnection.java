package org.cnv.shr.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public class DbConnection
{
	public DbConnection() throws ClassNotFoundException
	{
		Class.forName("org.sqlite.JDBC");
	}
	
	public Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:sqlite:" + Services.settings.getDbFile());
	}
	
	public void initialize() throws SQLException, IOException
	{
		HashSet<String> currentTables = Initialization.getCurrentTables();
		
		if (	!currentTables.contains("FILE") ||
				!currentTables.contains("PATH") ||
				!currentTables.contains("ROOT") ||
				!currentTables.contains("MACHINE") ||
				!currentTables.contains("KEY"))
		{
			Initialization.createDb();
		}
	}

	public Machine getMachine(String ip, int port)
	{
		return Machines.getMachine(ip, port);
	}
	public void addMachine(Machine m) throws SQLException
	{
		Machines.addMachine(m);
	}
	public void addRoot(Machine m, RootDirectory root) throws SQLException
	{
		Machines.addRoot(m, root);
	}
	public void addFiles(RootDirectory directory, List<SharedFile> files) throws SQLException
	{
		Files.addFiles(directory, files);
	}
}
