
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class DbRoots
{
	private static final QueryWrapper INSERT1 = new QueryWrapper("insert into IGNORE_PATTERN values (DEFAULT, ?, ?);");
	private static final QueryWrapper SELECT7 = new QueryWrapper("select PATTERN from IGNORE_PATTERN where RID=?;");
	private static final QueryWrapper SELECT6 = new QueryWrapper("select * from ROOT where RNAME=? and MID=?;");
	private static final QueryWrapper SELECT5 = new QueryWrapper("select * from ROOT where PELEM=?;");
	private static final QueryWrapper SELECT4 = new QueryWrapper("select * from ROOT where ROOT.IS_LOCAL = true;");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select * from ROOT where ROOT.MID = ?;");
	private static final QueryWrapper SELECT2 = new QueryWrapper("select count(F_ID) as number from SFILE where ROOT = ?;");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select sum(FSIZE) as totalsize from SFILE where ROOT = ?;");
	private static final QueryWrapper DELETE4 = new QueryWrapper("delete from IGNORE_PATTERN where RID=?;");
	private static final QueryWrapper DELETE3 = new QueryWrapper("delete from ROOT where R_ID=?;");
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete from SFILE where ROOT=?;");
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from ROOT_CONTAINS where ROOT_CONTAINS.RID=?;");
	
	public static long getTotalFileSize(RootDirectory d)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1))
		{
			stmt.setInt(1, d.getId());
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (executeQuery.next())
				{
					return executeQuery.getLong("totalsize");
				}
				return -1;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get file size of " + d, e);
			return -1;
		}
	}

	public static long getNumberOfFiles(RootDirectory d)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
		{
			stmt.setInt(1, d.getId());
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				if (executeQuery.next())
				{
					return executeQuery.getLong("number");
				}
				return -1;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to count files in " + d, e);
			return -1;
		}
	}
	
	public static DbIterator<RootDirectory> list(Machine machine)
	{
		try
		{
			ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			StatementWrapper prepareStatement = c.prepareStatement(SELECT3);
			prepareStatement.setInt(1,  machine.getId());
			return new DbIterator<RootDirectory>(c, 
					prepareStatement.executeQuery(),
					machine.isLocal() ? DbTables.DbObjects.LROOT : DbTables.DbObjects.RROOT, 
					new DbLocals().setObject(machine));
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list " + machine, e);
			return new DbIterator.NullIterator<>();
		}
	}
	
	public static DbIterator<LocalDirectory> listLocals()
	{
		try
		{
			ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
			return new DbIterator<LocalDirectory>(c, 
					c.prepareStatement(SELECT4).executeQuery(),
					DbTables.DbObjects.LROOT, 
					new DbLocals().setObject(Services.localMachine));
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list locals", e);
			return new DbIterator.NullIterator<>();
		}
	}

	public static LocalDirectory getLocal(String path)
	{
		PathElement pathElement = DbPaths.getPathElement(path);
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper prepareStatement = c.prepareStatement(SELECT5);)
		{
			prepareStatement.setLong(1, pathElement.getId());
			try (ResultSet executeQuery = prepareStatement.executeQuery();)
			{
				if (executeQuery.next())
				{
					LocalDirectory local = (LocalDirectory) DbTables.DbObjects.LROOT.allocate(executeQuery);
					local.fill(c, executeQuery, new DbLocals().setObject(Services.localMachine).setObject(pathElement));
					return local;
				}
			}
			return null;
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get local by path", e);
			return null;
		}
	}

	public static LocalDirectory getLocalByName(String rootName)
	{
		return (LocalDirectory) getRoot(Services.localMachine, rootName);
	}

	public static RootDirectory getRoot(Machine machine, String name)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper prepareStatement = c.prepareStatement(SELECT6);)
		{
			prepareStatement.setString(1, name);
			prepareStatement.setInt(2, machine.getId());
			try (ResultSet executeQuery = prepareStatement.executeQuery();)
			{
				DbObjects o = machine.isLocal() ? DbTables.DbObjects.LROOT : DbTables.DbObjects.RROOT;
				
				if (!executeQuery.next())
				{
					return null;
				}
				DbObject allocate = o.allocate(executeQuery);
				allocate.fill(c, executeQuery, new DbLocals().setObject(machine));
				return (RootDirectory) allocate;
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get root by name", e);
			return null;
		}
	}
	
	public static void deleteRoot(RootDirectory root)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper s1 = c.prepareStatement(DELETE1);
				StatementWrapper s2 = c.prepareStatement(DELETE2);
				StatementWrapper s3 = c.prepareStatement(DELETE3);)
		{
			s1.setInt(1, root.getId());
			s2.setInt(1, root.getId());
			s3.setInt(1, root.getId());
			s1.execute();
			s2.execute();
			s3.execute();
			
			Services.notifications.localsChanged();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete root", e);
		}
		DbPaths.removeUnusedPaths();
	}
        
	
	public static class IgnorePatterns
	{
		private final String[] patterns;
		long minFileSize = -1;
		long maxFileSize = -1;
		
		IgnorePatterns(String[] patterns, long min, long max)
		{
			this.patterns = patterns;
			minFileSize = min;
			maxFileSize = max;
		}
		
		public String[] getPatterns()
		{
			return this.patterns;
		}
		
		public boolean blocks(Path path)
		{
			String pathString = path.toString();
			for (String pattern : patterns)
			{
				if (pathString.contains(pattern))
				{
					return true;
				}
			}
			if (Files.isDirectory(path))
			{
				return false;
			}
			if (!Files.isRegularFile(path))
			{
				return true;
			}
			try
			{
				long fileSize = Files.size(path);
				if (minFileSize >= 0 && fileSize < minFileSize)
				{
					return true;
				}
				if (maxFileSize >= 0 && fileSize > maxFileSize)
				{
					return true;
				}
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to get file size.", e);
				return true;
			}
			return false;
		}
	}
	
	private static final String[] DUMMY = new String[0];
	public static IgnorePatterns getIgnores(LocalDirectory local)
	{
		LinkedList<String> returnValue = new LinkedList<>();
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper s1 = c.prepareStatement(SELECT7);)
		{
			s1.setInt(1, local.getId());
			try (ResultSet results = s1.executeQuery();)
			{
				while (results.next())
				{
					String string = results.getString(1);
					if (string.length() == 0)
					{
						continue;
					}
					returnValue.add(string);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get ignores.", e);
		}
		return new IgnorePatterns(returnValue.toArray(DUMMY), local.getMinFileSize(), local.getMaxFileSize());
	}

	public static void setIgnores(LocalDirectory local, String[] ignores)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection(); StatementWrapper s1 = c.prepareStatement(DELETE4);)
		{
			s1.setInt(1, local.getId());
			s1.execute();
		}
		catch (SQLException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete old ignores.", ex);
		}
		HashSet<String> ignoresAdded = new HashSet<>();
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection(); 
				 StatementWrapper s1 = c.prepareStatement(INSERT1);)
		{
			for (String ignore : ignores)
			{
				ignore = ignore.trim();
				if (ignore.length() > 1024)
				{
					LogWrapper.getLogger().info("Unable to add ignore because it is bigger than the maximum ignore pattern: " + ignore);
					continue;
				}
				if (ignore.length() <= 0)
				{
					LogWrapper.getLogger().info("Unable to add ignore because it is the emptry string.");
					continue;
				}
				if (!ignoresAdded.add(ignore))
				{
					continue;
				}
				s1.setInt(1, local.getId());
				s1.setString(2, ignore);
				s1.execute();
			}
		}
		catch (SQLException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to set new ignores.", ex);
		}
	}
}
