package org.cnv.shr.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.Misc;

public class DbConnection
{
	// Should be based on thread...
	private Connection c;
	
	public DbConnection() throws ClassNotFoundException, SQLException, IOException
	{
		Class.forName("org.sqlite.JDBC");
		
		Misc.ensureDirectory(Services.settings.getDbFile(), true);
		c = DriverManager.getConnection("jdbc:sqlite:" + Services.settings.getDbFile());

		HashSet<String> currentTables = Initialization.getCurrentTables(c);
		
		if (	!currentTables.contains("FILE") ||
				!currentTables.contains("PATH") ||
				!currentTables.contains("ROOT") ||
				!currentTables.contains("MACHINE") ||
				!currentTables.contains("KEY"))
		{
			Services.logger.logStream.println("Creating database.");
			Initialization.createDb(c);
		}
	}
	
	public void close()
	{
		try
		{
			c.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace(Services.logger.logStream);
			Services.logger.logStream.println("Unable to open database.");
			Main.quit();
		}
	}

	public Machine getMachine(String ip, int port)
	{
		return Machines.getMachine(c, ip, port);
	}
	public void addMachine(Machine m) throws SQLException
	{
		Machines.addMachine(c, m);
	}
	public void addRoot(Machine m, RootDirectory root) throws SQLException
	{
		Machines.addRoot(c, m, root);
	}
	public void addFiles(RootDirectory directory, List<SharedFile> files) throws SQLException
	{
		Files.addFiles(c, directory, files);
	}

	public List<Machine> getMachines()
	{
		try
		{
			return Machines.getRemotes(c);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get remotes.");
			e.printStackTrace(Services.logger.logStream);
			return new LinkedList<Machine>();
		}
	}
}
