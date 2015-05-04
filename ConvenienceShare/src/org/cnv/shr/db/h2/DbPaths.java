package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.DbConnection;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class DbPaths
{
	public static int ROOT_ID = 0;

	
	
	
	
	public static String getPath(Connection c, int pid) throws SQLException {
		RStringBuilder builder = new RStringBuilder();
		try (PreparedStatement stmt = c.prepareStatement("select PELEMENT, PARENT from PATH where PELEMENT = ?;"))
		{
			do
			{
				stmt.setInt(1,  pid);
				ResultSet executeQuery = stmt.executeQuery();
				builder.preppend(executeQuery.getString(1));
				pid = executeQuery.getInt(2);
			} while (pid != ROOT_ID);
		}
		return builder.toString();
	}
	
	
	public static PathElement getPathElement(Connection c, PathElement[] pathElems) throws SQLException
	{
		int pid = ROOT_ID;
		boolean exists = true;
		int elemsIndx = 0;

		try (PreparedStatement existsStmt = c.prepareStatement("select P_ID from PATH where PARENT=? and PELEMENT=?;");
			 PreparedStatement createStmt = c.prepareStatement("insert into PATH(PARENT, PELEMENT) values(?, ?);"))
		{
			while (elemsIndx < pathElems.length)
			{
				if (!exists)
				{
					createStmt.setInt   (1, pid);
					createStmt.setString(2, pathElems[elemsIndx]);
					createStmt.execute();
				}
				
				existsStmt.setInt(1, pid);
				existsStmt.setString(2, pathElems[elemsIndx]);
				ResultSet results = existsStmt.executeQuery();
				if (!results.next())
				{
					exists = false;
					continue;
				}
				
				pid = results.getInt(1);
				elemsIndx++;
			}
		}
		return pid;
	}
	
	public static DbIterator<PathElement> listPathElements(Connection c, RootDirectory root, PathElement parent)
	{
		return null;
	}


	public static void pathLiesIn(Connection c, PathElement element, LocalDirectory local) throws SQLException
	{
		
	}
	public static void pathDoesNotLieIn(Connection c, PathElement element, LocalDirectory local) throws SQLException
	{
		
	}
}
