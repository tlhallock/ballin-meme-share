package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class DbPaths
{
	private static final QueryWrapper DELETE = new QueryWrapper("delete from PELEM as del"
											+ " where not exists (select R_ID from ROOT where ROOT.PELEM = del.P_ID) "
											+ " and not exists (select F_ID from SFILE where SFILE.PELEM = del.P_ID)"
											+ " and not exists (select RID from ROOT_CONTAINS where ROOTS_CONTAINS.PELEM = del.P_ID)"
											+ " and not exists (select P_ID from PELEM as child where child.PARENT=del.P_ID);");
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete ROOT_CONTAINS where RID=? and PELEM=?;");
	private static final QueryWrapper INSERT1 = new QueryWrapper("insert into ROOT_CONTAINS values (?, ?);");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select count(RID) from ROOT_CONTAINS where RID=? and PELEM=?;");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select PELEM.P_ID, PELEM.PARENT, PELEM.BROKEN, PELEM.PELEM from PELEM          " + 
										 "join ROOT_CONTAINS on ROOT_CONTAINS.RID=? and ROOT_CONTAINS.PELEM = PELEM.P_ID " + 
										 "where PELEM.PARENT = ?;                                                        ");
	private static final QueryWrapper INSERT2 = new QueryWrapper("insert into PELEM(PARENT, BROKEN, PELEM) values(?, ?, ?);");
	private static final QueryWrapper INSERT3 = new QueryWrapper("select P_ID from PELEM where PARENT=? and PELEM=?;");
	private static final QueryWrapper SELECT2 = new QueryWrapper("select PARENT, PELEM from PATH where P_ID = ?;");
	
	
	
	public static PathElement ROOT = new PathElement(null, 0, "")
	{
		@Override
		public PathElement getParent()
		{
			return ROOT;
		}
		@Override
		public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException {}
	};

	public static PathElement getPathElement(int pid)
	{
		RStringBuilder builder = new RStringBuilder();
		
		ArrayList<Integer> ids   = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();

		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
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
			Services.logger.print(e);
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
		long pid = root.getId();
		boolean exists = true;
		int elemsIdx = 0;

		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			 StatementWrapper existsStmt = c.prepareStatement(INSERT3);
			 StatementWrapper createStmt = c.prepareStatement(INSERT2, Statement.RETURN_GENERATED_KEYS))
		{
			while (elemsIdx < pathElems.length)
			{
				if (exists)
				{
					existsStmt.setLong(1, pid);
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
					createStmt.setLong(1, pid);
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
			Services.logger.print(e);
		}
	}
	
	public static DbIterator<PathElement> listPathElements(RootDirectory root, PathElement parent)
	{
		try
		{
			ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			StatementWrapper stmt = c.prepareStatement(
				SELECT3);
			stmt.setInt(1, root.getId());
			stmt.setLong(2, parent.getId());
			return new DbIterator<PathElement>(c, stmt.executeQuery(), DbObjects.PELEM, new DbLocals()
				.setObject(root)
				.setObject(parent));
		}
		catch (SQLException ex)
		{
			Services.logger.print(ex);
			return new DbIterator.NullIterator<PathElement>();
		}
	}

	public static void pathLiesIn(PathElement element, RootDirectory local)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper select = c.prepareStatement(SELECT1);
				StatementWrapper update = c.prepareStatement(INSERT1);)
		{
			while (element.getParent() != element)
			{
				select.setInt(1, local.getId());
				select.setLong(2, element.getId());
				ResultSet results = select.executeQuery();
				if (results.next() && results.getInt(1) > 0)
				{
					return;
				}
				update.setInt(1, local.getId());
				update.setLong(2, element.getId());
				update.execute();
//				element = element.getParent();
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
	public static void pathDoesNotLieIn(PathElement element, RootDirectory local)
	{
		if (!element.isBroken() && element.getName().equals("/") && element.getParent() == DbPaths.ROOT)
		{
			return;
		}
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE2);)
		{
			while (element.getParent() != element)
			{
				stmt.setInt(1, local.getId());
				stmt.setLong(2, element.getId());
				stmt.execute();
				
				element = element.getParent();
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
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
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE);)
		{
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
}
