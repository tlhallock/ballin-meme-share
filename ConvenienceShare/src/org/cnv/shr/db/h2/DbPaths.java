
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
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbRoots.IgnorePatterns;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class DbPaths
{
	// This statement was problematic when it just deleted all records all at once, as it has h2 concurrency issues.
	// That is why it is broken up with a limit
	private static final QueryWrapper DELETE_SOME_UNUSED = new QueryWrapper("delete from PELEM "
											+ " where PELEM.P_ID <> 0 "
											+ " and not exists (select RID from ROOT_CONTAINS where ROOT_CONTAINS.PELEM = PELEM.P_ID limit 1)"
											+ " limit 10;");
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete ROOT_CONTAINS where RID=? and PELEM=?;");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select PELEM.P_ID, PELEM.PARENT, PELEM.BROKEN, PELEM.PELEM from PELEM          " + 
										 "join ROOT_CONTAINS on ROOT_CONTAINS.RID=? and ROOT_CONTAINS.PELEM = PELEM.P_ID " + 
										 "where PELEM.PARENT = ?;                                                        ");
	private static final QueryWrapper INSERT2 = new QueryWrapper("insert into PELEM(PARENT, BROKEN, PELEM) values(?, ?, ?);");
	private static final QueryWrapper INSERT3 = new QueryWrapper("select P_ID from PELEM where PARENT=? and PELEM=?;");
	private static final QueryWrapper SELECT2 = new QueryWrapper("select * from PELEM where P_ID = ?;");
	private static final QueryWrapper LISTALL = new QueryWrapper("select PELEM from ROOT_CONTAINS where ROOT_CONTAINS.RID=?;");
	

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

	public static PathElement getPathElement(Path p, boolean directory) throws IOException
	{
		// TODO: native Io
		return getPathElement(p.toFile().getCanonicalPath(), directory);
	}
	
	public static PathElement getPathElement(long pid, DbLocals locals)
	{
		if (locals == null)
		{
			locals = new DbLocals();
		}
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
		{
				stmt.setLong(1, pid);
				try (ResultSet executeQuery = stmt.executeQuery();)
				{
					if (!executeQuery.next())
					{
						throw new SQLException("This should not happen");
					}
					PathElement element = (PathElement) DbObjects.PELEM.allocate(executeQuery);
					element.fill(c, executeQuery, locals);
					return element;
				}
			}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get path element by id " + pid, e);
		}
		return null;
	}

//	public static String getPathString(long pid)
//	{
//		return "Needs to be implemented.";
//	}
	
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

	public static PathElement getPathElement(LocalDirectory local, String fsPath, boolean directory)
	{
		String relPath = fsPath.substring(local.getPathElement().getFullPath().length());
		return getPathElement(relPath, directory);
	}

	public static PathElement getPathElement(String relPath, boolean directory)
	{
		return getPathElement(ROOT, relPath, directory);
	}

	public static PathElement getPathElement(PathElement parentId, String relPath, boolean directory)
	{
		if (relPath.length() == 0 || relPath.equals(".") || relPath.equals("./") || relPath.equals("/"))
		{
			return ROOT;
		}
		PathElement[] broken = PathBreaker.breakPath(parentId, relPath, directory);
		setPathElementIds(parentId, broken);
		return broken[broken.length-1];
	}

	public static void removeUnusedPaths()
	{
		int numDeleted = -1;
		do
		{
			try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection(); StatementWrapper stmt = c.prepareStatement(DELETE_SOME_UNUSED);)
			{
				numDeleted = stmt.executeUpdate();
				LogWrapper.getLogger().fine("Removed " + numDeleted + " unused paths.");
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to remove unused paths.", e);
				return;
			}
		} while (numDeleted > 0);
	}

	public static void cleanIgnores(LocalDirectory local)
	{
		PathElement pathElement = local.getPathElement();
		Path rootPath = Paths.get(pathElement.getFullPath());
		HashSet<Long> keep = new HashSet<>();
		while (pathElement.getId() != DbPaths.ROOT.getId())
		{
			DbPaths.pathLiesIn(pathElement, local); // Can't hurt.
			keep.add(pathElement.getId());
			pathElement = pathElement.getParent();
		}
		IgnorePatterns p = DbRoots.getIgnores(local);
		DbLocals locals = new DbLocals();

		class RemoverRunnable implements Runnable
		{
			LinkedBlockingDeque<PathElement> queue = new LinkedBlockingDeque<>();
			boolean done;
			boolean started;

			@Override
			public synchronized void run()
			{
				try
				{
					started = true;
					int count = 0;
					while (!done || !queue.isEmpty())
					{
						PathElement takeFirst = queue.pollFirst(10, TimeUnit.SECONDS);
						if (takeFirst != null)
						{
							LogWrapper.getLogger().fine("removing " + takeFirst.getFullPath());
							DbPaths.pathDoesNotLieIn(takeFirst, local);
							count = 0;
						}
						if (count++ > 10 * 60)
						{
							break;
						}
					}
				}
				catch (InterruptedException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
				}
			}
		}
		RemoverRunnable r = new RemoverRunnable();
		Services.userThreads.execute(r);
		while (!r.started)
		{
			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
				return;
			}
		}

		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
		{
			c.setAutoCommit(true);
			try (StatementWrapper s1 = c.prepareStatement(LISTALL);)
			{
				s1.setFetchSize(50);
				s1.setInt(1, local.getId());
				try (ResultSet results = s1.executeQuery())
				{
					while (results.next())
					{
						long pathId = results.getLong(1);
						if (keep.contains(pathId))
						{
							continue;
						}
						PathElement element = DbPaths.getPathElement(pathId, locals);
						if (element.isBroken())
						{
							continue;
						}
						if (!p.blocks(rootPath.resolve(element.getFullPath())))
						{
							continue;
						}
						if (element.getId() == 0)
						{
							continue;
						}
						r.queue.offerLast(element);
					}
				}
			}
			finally
			{
				c.setAutoCommit(false);
			}
		}
		catch (SQLException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to clean based on ignores...", ex);
		}
		r.done = true;
		synchronized (r)
		{
			LogWrapper.getLogger().info("Remover runnable done.");
		}
		DbPaths.removeUnusedPaths();
	}
}
