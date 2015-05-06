package org.cnv.shr.db.h2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class DbRoots
{

	public static long getTotalFileSize(RootDirectory d)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select sum(FSIZE) as totalsize from SFILE where ROOT = ?;"))
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
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to get file size of " + d);
			e.printStackTrace(Services.logger.logStream);
			return -1;
		}
	}

	public static long getNumberOfFiles(RootDirectory d)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select count(F_ID) as number from SFILE where ROOT = ?;"))
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
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to count files in " + d);
			e.printStackTrace(Services.logger.logStream);
			return -1;
		}
	}
	
	public static DbIterator<RootDirectory> list(Machine machine)
	{
		Connection c = Services.h2DbCache.getConnection();
		try
		{
			PreparedStatement prepareStatement = c.prepareStatement("select * from ROOT where ROOT.MID = ?;");
			prepareStatement.setInt(1,  machine.getId());
			return new DbIterator<RootDirectory>(c, 
					prepareStatement.executeQuery(),
					machine.isLocal() ? DbTables.DbObjects.LROOT : DbTables.DbObjects.RROOT, 
					new DbLocals().setObject(machine));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new DbIterator.NullIterator<>();
		}
	}
	
	public static DbIterator<LocalDirectory> listLocals()
	{
		// return list(Services.localMachine));
		Connection c = Services.h2DbCache.getConnection();
		try
		{
			return new DbIterator<LocalDirectory>(c, 
					c.prepareStatement("select * from ROOT where ROOT.IS_LOCAL = true;").executeQuery(),
					DbTables.DbObjects.LROOT, 
					new DbLocals().setObject(Services.localMachine));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new DbIterator.NullIterator<>();
		}
	}

	public static LocalDirectory getLocal(String path)
	{
		PathElement pathElement = DbPaths.getPathElement(path);
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement prepareStatement = c.prepareStatement("select * from ROOT where PELEM=?;");)
		{
			prepareStatement.setInt(1, pathElement.getId());
			ResultSet executeQuery = prepareStatement.executeQuery();
			if (executeQuery.next())
			{
				LocalDirectory local = (LocalDirectory) DbTables.DbObjects.LROOT.allocate(executeQuery);
				local.fill(c, executeQuery, new DbLocals().setObject(Services.localMachine).setObject(pathElement));
				return local;
			}
			LocalDirectory local = new LocalDirectory(pathElement);
			local.save();
			DbPaths.pathLiesIn(pathElement, local);
			return local;
		}
		catch (SQLException | IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static LocalDirectory getLocalByName(String rootName)
	{
		return (LocalDirectory) getRoot(Services.localMachine, rootName);
	}

	public static RootDirectory getRoot(Machine machine, String name)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement prepareStatement = c.prepareStatement("select * from ROOT where RNAME=? and MID=?;");)
		{
			prepareStatement.setString(1, name);
			prepareStatement.setInt(2, machine.getId());
			ResultSet executeQuery = prepareStatement.executeQuery();

			DbObjects o = machine.isLocal() ? DbTables.DbObjects.LROOT : DbTables.DbObjects.RROOT;
			
			if (!executeQuery.next())
			{
				return null;
			}
			DbObject allocate = o.allocate(executeQuery);
			allocate.fill(c, executeQuery, new DbLocals().setObject(machine));
			return (RootDirectory) allocate;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
