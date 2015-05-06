package org.cnv.shr.db.h2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

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

//	public static void addRoot(Connection c, RootDirectory root) throws SQLException
//	{
//		try (PreparedStatement stmt = c.prepareStatement(
//				"insert into ROOT(PATH, MID, LOCAL, TAGS, DESC)       " +
//				"values (?, ?, ?, ?, ?);                              "))
//		{
//			int ndx = 1;
//			stmt.setString(ndx++, root.getCanonicalPath());
//			stmt.setInt	  (ndx++, root.getMachine().getDbId());
//			stmt.setInt   (ndx++, root.isLocal() ? 1 : 0);
//			stmt.setString(ndx++, root.getTags());
//			stmt.setString(ndx++, root.getDescription());
//			stmt.execute();
//		}
//	}
//	
//	public static void updateRoot(Connection c, RootDirectory root) throws SQLException
//	{
//		try (PreparedStatement stmt = c.prepareStatement(
//					"update ROOT                         " +
//					"set NFILES=?,SPACE=?,TAGS=?,DESC=?  " +
//					"where MID = ? and PATH=?;           "))
//		{
//			int ndx = 1;
//			stmt.setLong  (ndx++, root.numFiles());
//			stmt.setLong  (ndx++, root.diskSpace());
//			stmt.setString(ndx++, root.getTags());
//			stmt.setString(ndx++, root.getDescription());
//			
//			stmt.setInt   (ndx++, root.getMachine().getDbId());
//			stmt.setString(ndx++, root.getCanonicalPath());
//			stmt.execute();
//		}
//	}
//	
//	public static void removeRoot(Connection c, RootDirectory root) throws SQLException
//	{
//		try (PreparedStatement stmt = c.prepareStatement(
//				"delete from ROOT           " +
//				"where MID = ? and PATH=?;  "))
//		{
//			int ndx = 1;
//			stmt.setLong  (ndx++, root.numFiles());
//			stmt.setLong  (ndx++, root.diskSpace());
//			stmt.setInt   (ndx++, root.getMachine().getDbId());
//			stmt.setString(ndx++, root.getCanonicalPath());
//			stmt.execute();
//		}
//	}
	
	public static DbIterator<RemoteDirectory> listRemoteDirectories(Machine machine)
	{
		Connection c = Services.h2DbCache.getConnection();
		try
		{
			return new DbIterator<RemoteDirectory>(c, 
					c.prepareStatement("select * from ROOT where ROOT.MID = " + machine.getId() + ";").executeQuery(),
					DbTables.DbObjects.LROOT, 
					new DbLocals().setObject(machine));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new DbIterator.NullIterator<RemoteDirectory>();
		}
	}
	
	public static DbIterator<LocalDirectory> listLocals()
	{
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
			return new DbIterator.NullIterator<LocalDirectory>();
		}
	}

	public static Iterator<SharedFile> list(RootDirectory directory)
	{
		// TODO Auto-generated method stub
		return null;
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
		return null;
	}

	public static RemoteDirectory getRemote(Machine machine, String name)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
