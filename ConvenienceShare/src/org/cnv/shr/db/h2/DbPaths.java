
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.db.h2;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class DbPaths
{
	private static final QueryWrapper DELETE = new QueryWrapper("delete from PELEM "
											+ " where not PELEM.P_ID = 0 "
											+ " and not exists (select RID from ROOT_CONTAINS where ROOT_CONTAINS.PELEM = PELEM.P_ID)");
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete ROOT_CONTAINS where RID=? and PELEM=?;");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select PELEM.P_ID, PELEM.PARENT, PELEM.BROKEN, PELEM.PELEM from PELEM          " + 
										 "join ROOT_CONTAINS on ROOT_CONTAINS.RID=? and ROOT_CONTAINS.PELEM = PELEM.P_ID " + 
										 "where PELEM.PARENT = ?;                                                        ");
	private static final QueryWrapper INSERT2 = new QueryWrapper("insert into PELEM(PARENT, BROKEN, PELEM) values(?, ?, ?);");
	private static final QueryWrapper INSERT3 = new QueryWrapper("select P_ID from PELEM where PARENT=? and PELEM=?;");
	private static final QueryWrapper SELECT2 = new QueryWrapper("select PARENT, PELEM from PATH where P_ID = ?;");
	

//	private static final QueryWrapper SELECT1 = new QueryWrapper("select count(RID) from ROOT_CONTAINS where RID=? and PELEM=?;");
//	private static final QueryWrapper INSERT1 = new QueryWrapper("insert into ROOT_CONTAINS values (?, ?);");
	
	private static final QueryWrapper MERGE1  = new QueryWrapper("merge into ROOT_CONTAINS key(RID, PELEM) values (?, ?);");
	
	
	
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

	public static PathElement getPathElement(Path p) throws IOException
	{
		// TODO: native Io
		return getPathElement(p.toFile().getCanonicalPath());
	}
	
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
				try (ResultSet executeQuery = stmt.executeQuery();)
				{
					values.add(executeQuery.getString(1));
					ids.add(pid = executeQuery.getInt(2));
				}
			}
			while (pid != ROOT.getId());
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get path element by id " + pid, e);
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
					try (ResultSet results = existsStmt.executeQuery();)
					{
						if (!results.next())
						{
							exists = false;
							continue;
						}

						pathElems[elemsIdx].setId(pid = results.getInt(1));
						elemsIdx++;
					}
				}
				else
				{
					createStmt.setLong(1, pid);
					createStmt.setBoolean(2, pathElems[elemsIdx].isBroken());
					createStmt.setString(3, pathElems[elemsIdx].getName());
					createStmt.executeUpdate();
					try (ResultSet generatedKeys = createStmt.getGeneratedKeys();)
					{
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
					}
					elemsIdx++;
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to set path element ids", e);
		}
	}
	
	public static DbIterator<PathElement> listPathElements(RootDirectory root, PathElement parent)
	{
		try
		{
			ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			StatementWrapper stmt = c.prepareStatement(SELECT3);
			stmt.setInt(1, root.getId());
			stmt.setLong(2, parent.getId());
			return new DbIterator<PathElement>(c, stmt.executeQuery(), DbObjects.PELEM, new DbLocals()
				.setObject(root)
				.setObject(parent));
		}
		catch (SQLException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list path elements of " + parent, ex);
			return new DbIterator.NullIterator<PathElement>();
		}
	}

	public static void pathLiesIn(PathElement element, RootDirectory local)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper merge = c.prepareStatement(MERGE1);)
		{
			PathElement prev;
			do
			{
				merge.setInt(1, local.getId());
				merge.setLong(2, element.getId());
				merge.execute();
				if (merge.getUpdateCount() == 0)
				{
					return;
				}
				element = (prev = element).getParent();
			} while (prev.getId() != element.getId());
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save to root contains path.", e);
		}
	}
	public static void pathDoesNotLieIn(PathElement element, RootDirectory local)
	{
		if (element.getId() == 0)
		{
			return;
		}
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE2);)
		{
			stmt.setInt( 1, local.getId());
			stmt.setLong(2, element.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove path from root: " + element, e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove unused paths.", e);
		}
	}
}
