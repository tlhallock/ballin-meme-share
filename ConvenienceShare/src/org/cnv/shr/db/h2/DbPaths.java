package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class DbPaths
{
	public static PathElement ROOT = new PathElement(null, 0, "")
	{
		public PathElement getParent()
		{
			return ROOT;
		}
		@Override
		public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException {}
	};

	public static PathElement getPathElement(int pid)
	{
		Connection c = Services.h2DbCache.getConnection();
		RStringBuilder builder = new RStringBuilder();
		
		
		ArrayList<Integer> ids   = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		
		try (PreparedStatement stmt = c.prepareStatement("select PARENT, PELEM from PATH where P_ID = ?;"))
		{
			do
			{
				stmt.setInt(1, pid);
				ResultSet executeQuery = stmt.executeQuery();
				values.add(executeQuery.getString(1));
				ids.add(pid = executeQuery.getInt(2));
			}
			while (pid != ROOT.getId());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		PathElement current = ROOT;
		for (int i = 0; i < ids.size(); i++)
		{
			current = new PathElement(current, ids.get(i), values.get(i));
		}
		
		return current;
	}
	
	public static void setPathElementIds(PathElement root, PathElement[] pathElems)
	{
		Connection c = Services.h2DbCache.getConnection();
		int pid = root.getId();
		boolean exists = true;
		int elemsIdx = 0;

		try (PreparedStatement existsStmt = c.prepareStatement("select P_ID from PELEM where PARENT=? and PELEM=?;");
			 PreparedStatement createStmt = c.prepareStatement("insert into PELEM(PARENT, BROKEN, PELEM) values(?, ?, ?);", Statement.RETURN_GENERATED_KEYS))
		{
			while (elemsIdx < pathElems.length)
			{
				if (exists)
				{
					existsStmt.setInt(1, pid);
					existsStmt.setString(2, pathElems[elemsIdx].getName());
					ResultSet results = existsStmt.executeQuery();
					if (!results.next())
					{
						exists = false;
						continue;
					}

					pathElems[elemsIdx].setId(pid = results.getInt(1));
					elemsIdx++;
				}
				else
				{
					createStmt.setInt(1, pid);
					createStmt.setBoolean(2, pathElems[elemsIdx].isBroken());
					createStmt.setString(3, pathElems[elemsIdx].getName());
					createStmt.executeUpdate();
					ResultSet generatedKeys = createStmt.getGeneratedKeys();
					if (generatedKeys.next())
					{
						pathElems[elemsIdx].setId(pid = generatedKeys.getInt(1));
					}
					else
					{
						if (pathElems[elemsIdx].getId() == null)
						{
							throw new RuntimeException("Unable to create path: " 
									+ PathBreaker.join(pathElems) 
									+ "[" + pathElems[elemsIdx].getName() + "]");
						}
					}
					elemsIdx++;
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public static DbIterator<PathElement> listPathElements(RootDirectory root, PathElement parent)
	{
		Connection c = Services.h2DbCache.getConnection();
		try
		{
			PreparedStatement stmt = c.prepareStatement(
				"select PELEM.P_ID, PELEM.PARENT, PELEM.BROKEN, PELEM.PELEM from PELEM          " + 
				"join ROOT_CONTAINS on ROOT_CONTAINS.RID=? and ROOT_CONTAINS.PELEM = PELEM.P_ID " + 
				"where PELEM.PARENT = ?;                                                        ");
			stmt.setInt(1, root.getId());
			stmt.setInt(2, parent.getId());
			return new DbIterator<PathElement>(c, stmt.executeQuery(), DbObjects.PELEM, new DbLocals()
				.setObject(root)
				.setObject(parent)
			);
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			return new DbIterator.NullIterator<PathElement>();
		}
	}

	public static void pathLiesIn(PathElement element, RootDirectory local)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement select = c.prepareStatement("select count(RID) from ROOT_CONTAINS where RID=? and PELEM=?;");
		     PreparedStatement update = c.prepareStatement("insert into ROOT_CONTAINS values (?, ?);");)
		{
			while (element.getParent() != element)
			{
				select.setInt(1, local.getId());
				select.setInt(2, element.getId());
				ResultSet results = select.executeQuery();
				if (results.next() && results.getInt(1) > 0)
				{
					return;
				}
				update.setInt(1, local.getId());
				update.setInt(2, element.getId());
				update.execute();
//				element = element.getParent();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	public static void pathDoesNotLieIn(PathElement element, RootDirectory local)
	{
		if (!element.isBroken() && element.getName().equals("/") && element.getParent() == DbPaths.ROOT)
		{
			return;
		}
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("delete ROOT_CONTAINS where RID=? and PELEM=?;");)
		{
			while (element.getParent() != element)
			{
				stmt.setInt(1, local.getId());
				stmt.setInt(2, element.getId());
				stmt.execute();
				
				element = element.getParent();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static PathElement getPathElement(LocalDirectory local, String fsPath)
	{
		String relPath = fsPath.substring(local.getPathElement().getFullPath().length());
		return getPathElement(relPath);
	}

	public static PathElement getPathElement(String relPath)
	{
		return getPathElement(ROOT, relPath);
	}

	public static PathElement getPathElement(PathElement parentId, String relPath)
	{
		if (relPath.length() == 0 || relPath.equals(".") || relPath.equals("./") || relPath.equals("/"))
		{
			return ROOT;
		}
		PathElement[] broken = PathBreaker.breakPath(parentId, relPath);
		setPathElementIds(parentId, broken);
		return broken[broken.length-1];
	}
	
	public static void removeUnusedPaths()
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("delete from PELEM "
				+ "where not exists (select R_ID from ROOT where ROOT.PELEM = PELEM.P_ID) "
				+ "and not exists (select F_ID from SFILE where SFILE.PELEM = PELEM.P_ID);");)
		{
			stmt.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}
