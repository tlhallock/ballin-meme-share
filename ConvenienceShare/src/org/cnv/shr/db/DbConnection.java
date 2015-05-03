package org.cnv.shr.db;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.Misc;

public class DbConnection
{
	
	private HashMap<Long, Connection> connections = new HashMap<>();
	
	
	static boolean SQLITE = false;
	
	// Should be based on thread...
	public DbConnection() throws ClassNotFoundException, SQLException, IOException
	{
		if (SQLITE)
		{
			Class.forName("org.sqlite.JDBC");
		}
		else
		{
			Class.forName("org.h2.Driver");
		}
		Misc.ensureDirectory(Services.settings.dbFile.get(), true);
		Connection c = getConnection();
		
//		HashSet<String> currentTables = Initialization.getCurrentTables(c);
//		
//		if (	!currentTables.contains("FILE") ||
//				!currentTables.contains("PATH") ||
//				!currentTables.contains("ROOT") ||
//				!currentTables.contains("MACHINE") ||
//				!currentTables.contains("KEY"))
//		{
			Services.logger.logStream.println("Creating database.");
			Initialization.createDb(c);
//		}
	}
	
	private Connection getConnection() throws SQLException
	{
		long id = Thread.currentThread().getId();
		id = 0;
		Connection returnValue = connections.get(id);
		if (returnValue == null)
		{
	        Connection conn;
	        if (SQLITE) 
	        {
	        	returnValue = DriverManager.getConnection("jdbc:sqlite:" + Services.settings.dbFile.get());
	        }
	        else
	        {
	        	returnValue = DriverManager.getConnection("jdbc:h2:~/test.db", "sa", "");
	        }
			
			connections.put(id, returnValue);
		}
		return returnValue;
	}
	
	public void close()
	{
		for (Connection c : connections.values())
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
	}

	public Machine getMachine(String identifier)
	{
		try
		{
			return SQL.getMachine(getConnection(), identifier);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to open database.");
			e.printStackTrace(Services.logger.logStream);
			return null;
		}
	}
//	public Machine getMachine(String ip, int port)
//	{
//		try
//		{
//			return Machines.getMachine(getConnection(), ip, port);
//		}
//		catch (SQLException e)
//		{
//			Services.logger.logStream.println("Unable to open database.");
//			e.printStackTrace(Services.logger.logStream);
//			return null;
//		}
//	}

	public void addMachine(Machine m) throws SQLException
	{
		SQL.addMachine(getConnection(), m);
	}

	public boolean addRoot(Machine m, RootDirectory root)
	{
		try
		{
			SQL.addRoot(getConnection(), m, root);
			return true;
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to add root " + root.getCanonicalPath());
			e.printStackTrace(Services.logger.logStream);
			return false;
		}
	}
	
	public RootDirectory getRoot(Machine machine, String path)
	{
		try
		{
			return SQL.getRoot(getConnection(), machine, path);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get root " + path);
			e.printStackTrace(Services.logger.logStream);
			return null;
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Db contains path not in filesystem: " + path);
			e.printStackTrace(Services.logger.logStream);
			return null;
		}
	}

	public void addFiles(RootDirectory directory, List<SharedFile> files)
	{
		try
		{
			SQL.addFiles(getConnection(), directory, files);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to files. ");
			e.printStackTrace(Services.logger.logStream);
			
			for (SharedFile file : files)
			{
				addFile(directory, file);
			}
		}
	}

	public void addFile(RootDirectory localDirectory, SharedFile newFile)
	{
		try
		{
			SQL.addFile(getConnection(), localDirectory, newFile);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to file " + newFile.getRelativePath());
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public LocalFile getFile(RootDirectory directory, String relPath, String name)
	{
		try
		{
			return SQL.getFile(getConnection(), directory, relPath, name);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to retreive file.");
			e.printStackTrace(Services.logger.logStream);
			return null;
		}
	}

	public LocalFile findLocalFile(LocalDirectory dir, File f)
	{
		try
		{
			String base    = f.getParentFile().getCanonicalPath();
			String name    = f.getName();
			String relPath;
			if (base.length() == dir.getCanonicalPath().length())
			{
				relPath = ".";
			}
			else
			{
				relPath = base.substring(dir.getCanonicalPath().length() + 1);
			}
			return getFile(dir, relPath, name);
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to get local file path.");
			e.printStackTrace(Services.logger.logStream);
			return null;
		}
	}
	
	public void updateFile(SharedFile f)
	{
		try
		{
			SQL.updateFile(getConnection(), f.getRootDirectory().getId(), f);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to update file " + f);
			e.printStackTrace(Services.logger.logStream);
		}
	}
	
	public void removeFile(SharedFile f)
	{
		try
		{
			Services.logger.logStream.println("Removing " + f);
			SQL.removeFile(getConnection(), f.getId());
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to update file " + f);
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public List<Machine> getRemoteMachines()
	{
		try
		{
			return SQL.getRemotes(getConnection());
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get remotes.");
			e.printStackTrace(Services.logger.logStream);
			return new LinkedList<Machine>();
		}
	}

	public List<LocalDirectory> getLocals()
	{
		try
		{
			return SQL.getLocals(getConnection());
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get locals.");
			e.printStackTrace(Services.logger.logStream);
			return new LinkedList<LocalDirectory>();
		}
	}

	public List<RemoteDirectory> getRemoteRoots(Machine machine)
	{
		try
		{
			return SQL.listRemotes(getConnection(), machine);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get locals.");
			e.printStackTrace(Services.logger.logStream);
			return new LinkedList<RemoteDirectory>();
		}
	}
	
	public List<? extends RootDirectory> listRoots(Machine machine)
	{
		if (machine.isLocal())
		{
			return getLocals();
		}
		else
		{
			return getRemoteRoots(machine);
		}
	}

	public Iterator<SharedFile> list(RootDirectory d)
	{
		try
		{
			return SQL.list(getConnection(), d);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get list local directory.");
			e.printStackTrace(Services.logger.logStream);
			return new Iterator<SharedFile>() {
				public boolean hasNext() { return false; }
				public SharedFile next() { return null; }
				public void remove() { }};
		}
	}

	public Integer getRootId(RootDirectory rootDirectory)
	{
		try
		{
			return SQL.getRootDirectoryId(getConnection(), rootDirectory.getMachine(), rootDirectory.getCanonicalPath());
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get id of directory " + rootDirectory);
			e.printStackTrace(Services.logger.logStream);
			return -1;
		}
	}

	public long countFiles(RootDirectory rootDirectory)
	{
		try
		{
			return SQL.getNumberOfFiles(getConnection(), rootDirectory);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to count files in " + rootDirectory);
			e.printStackTrace(Services.logger.logStream);
			return -1;
		}
	}

	public long countFileSize(RootDirectory rootDirectory)
	{
		try
		{
			return SQL.getTotalFileSize(getConnection(), rootDirectory);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get file size of " + rootDirectory);
			e.printStackTrace(Services.logger.logStream);
			return -1;
		}
	}
	
	public String getPath(int int1)
	{
		try
		{
			return SQL.getPath(getConnection(), int1);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to find path with id " + int1);
			e.printStackTrace(Services.logger.logStream);
			return "unkown";
		}
	}

	public void removeUnusedPaths()
	{
		try
		{
			SQL.removeUnusedPaths(getConnection());
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable remove unused paths.");
			e.printStackTrace(Services.logger.logStream);
		}
	}
	
	public String[] getIgnores(RootDirectory root)
	{
		return new String[] {"This feature not supported yet." };
	}
	public void setIgnores(RootDirectory root, String[] ignores)
	{
		for (String s : ignores)
		{
			if (s.trim().length() == 0)
			{
				continue;
			}
		}
	}
	
	public void debug(PrintStream ps)
	{
		DbConnection.printTable(ps, "PATH");
		DbConnection.printTable(ps, "FILE");
		DbConnection.printTable(ps, "MACHINE");
		DbConnection.printTable(ps, "KEY");
		DbConnection.printTable(ps, "ROOT");
		DbConnection.printTable(System.out, "PENDING");
		DbConnection.printTable(System.out, "IGNOREPATTERN");
		
		ps.println("Locals:");
		Services.locals.debug(ps);
		
		ps.println("Remotes: ");
		Services.remotes.debug(ps);
	}
	

	public static void printTable(PrintStream ps, String name)
	{
		try
		{
			Connection c = Services.db.getConnection();
//			new Exception().printStackTrace(ps);

			ps.println("Printing " + name);
			ps.println("----------------------------------------------");
			ResultSet executeQuery2 = c.prepareStatement("select * from " + name + ";").executeQuery();
			int ncols = executeQuery2.getMetaData().getColumnCount();
			for (int i = 1; i < ncols; i++)
			{
				ps.print(executeQuery2.getMetaData().getColumnName(i) + ",");
			}
			ps.println();
			while (executeQuery2.next())
			{
				for (int i = 1; i <= ncols; i++)
				{
					ps.print(executeQuery2.getObject(i) + ",");
				}
				ps.println();
			}
			ps.println("----------------------------------------------");
		}
		catch (Exception e)
		{
			e.printStackTrace(ps);
		}
	}

	public void updateDirectory(Machine machine, RootDirectory rootDirectory)
	{
		try
		{
			SQL.updateRoot(getConnection(), machine, rootDirectory);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to update directory " + rootDirectory);
			e.printStackTrace(Services.logger.logStream);
		}
	}

    public void deleteDb()
	{
    	Services.logger.logStream.println("Removing all data from database!!!!");
		try (Connection c = getConnection())
		{
			connections.clear();
			Initialization.clearDb(c, null);
			connections.clear();
			Initialization.createDb(c);
			connections.clear();

			addMachine(Services.localMachine);
			Services.locals.share(Services.settings.downloadsDirectory.get());
		}
		catch (SQLException | IOException e)
		{
			Services.logger.logStream.println("Unable to delete database.");
			e.printStackTrace(Services.logger.logStream);
		}
	}
}
