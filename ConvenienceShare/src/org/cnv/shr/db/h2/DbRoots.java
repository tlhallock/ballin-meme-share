package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;

public class DbRoots
{
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
	
	public static DbIterator<RemoteDirectory> listRemoteDirectories(Connection c, Machine machine)
	{
		try
		{
			return new DbIterator<RemoteDirectory>(c, 
					c.prepareStatement("select * from ROOT where ROOT.MID = " + machine.getId() + ";").executeQuery(),
					DbTables.DbObjects.LROOT, 
					new DbLocals().setObject(DbTables.DbObjects.RMACHINE, machine.getId(), machine));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new DbIterator.NullIterator<RemoteDirectory>();
		}
	}
	
	public static DbIterator<LocalDirectory> listLocals(Connection c)
	{
		try
		{
			return new DbIterator<LocalDirectory>(c, 
					c.prepareStatement("select * from ROOT where ROOT.LOCAL = 1;").executeQuery(),
					DbTables.DbObjects.LROOT, 
					new DbLocals().setObject(DbTables.DbObjects.LMACHINE, Services.localMachine.getId(), Services.localMachine));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new DbIterator.NullIterator<LocalDirectory>();
		}
	}
}
