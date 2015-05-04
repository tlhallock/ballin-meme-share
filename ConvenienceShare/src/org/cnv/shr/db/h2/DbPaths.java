package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.db.DbConnection;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class DbPaths
{
	public static PathElement ROOT = new PathElement(0, "");

	
	
	
	public static String getPath(int pid)
	{
		Connection c = Services.h2DbCache.getConnection();
		RStringBuilder builder = new RStringBuilder();
		try (PreparedStatement stmt = c.prepareStatement("select PELEMENT, PARENT from PATH where PELEMENT = ?;"))
		{
			do
			{
				stmt.setInt(1, pid);
				ResultSet executeQuery = stmt.executeQuery();
				builder.preppend(executeQuery.getString(1));
				pid = executeQuery.getInt(2);
			}
			while (pid != ROOT.getId());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	public static void getPathElementIds(PathElement[] pathElems)
	{
		Connection c = Services.h2DbCache.getConnection();
		LinkedList<PathElement> elems = new LinkedList<>();
		int pid = ROOT.getId();
		boolean exists = true;

		try (PreparedStatement existsStmt = c.prepareStatement("select P_ID from PATH where PARENT=? and PELEMENT=?;");
			 PreparedStatement createStmt = c.prepareStatement("insert into PATH(PARENT, PELEMENT) values(?, ?);"))
		{
			for (PathElement ele : pathElems)
			{
				String[] dbEles = ele.getDbValues();
				int elemsIdx = 0;
				while (elemsIdx < dbEles.length)
				{
					if (exists)
					{
						existsStmt.setInt(1, pid);
						existsStmt.setString(2, dbEles[elemsIdx]);
						ResultSet results = existsStmt.executeQuery();
						if (!results.next())
						{
							exists = false;
							continue;
						}

						pid = results.getInt(1);
						elemsIdx++;
					}
					else
					{
						createStmt.setInt(1, pid);
						createStmt.setString(2, dbEles[elemsIdx]);
						createStmt.executeUpdate();

						pid = createStmt.getGeneratedKeys().getInt(1);
					}
				}
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
