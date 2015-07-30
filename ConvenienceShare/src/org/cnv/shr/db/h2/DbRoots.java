
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
import org.cnv.shr.gui.DeleteRootProgress;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.RootDirectoryType;
import org.cnv.shr.util.LogWrapper;

public class DbRoots
{
	private static final QueryWrapper INSERT1 = new QueryWrapper("insert into IGNORE_PATTERN values (DEFAULT, ?, ?);");
	private static final QueryWrapper SELECT7 = new QueryWrapper("select PATTERN from IGNORE_PATTERN where RID=?;");
	private static final QueryWrapper SELECT8 = new QueryWrapper("select * from ROOT where RID=?;");
	private static final QueryWrapper SELECT6 = new QueryWrapper("select * from ROOT where RNAME=? and MID=?;");
	private static final QueryWrapper SELECT5 = new QueryWrapper("select * from ROOT where PATH=? and ROOT.TYPE in (" 
			+ RootDirectoryType.LOCAL.getDbValue() + ", " + RootDirectoryType.MIRROR.getDbValue() + ");");
	private static final QueryWrapper SELECT4 = new QueryWrapper("select * from ROOT where ROOT.TYPE in (" 
			+ RootDirectoryType.LOCAL.getDbValue() + ", " + RootDirectoryType.MIRROR.getDbValue() + ");");
	private static final QueryWrapper SELECT3 = new QueryWrapper("select * from ROOT where ROOT.MID = ?;");
	private static final QueryWrapper SELECT2 = new QueryWrapper("select count(F_ID) as number from SFILE where ROOT = ?;");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select sum(FSIZE) as totalsize from SFILE where ROOT = ?;");
	private static final QueryWrapper DELETE4 = new QueryWrapper("delete from IGNORE_PATTERN where RID=?;");
	private static final QueryWrapper DELETE3 = new QueryWrapper("delete from ROOT where R_ID=?;");
	
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
					DbObjects.ROOT,
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
					DbTables.DbObjects.ROOT, 
					new DbLocals().setObject(Services.localMachine));
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list locals", e);
			return new DbIterator.NullIterator<>();
		}
	}
//	public static RootDirectory getRoot(int id)
//	{
//		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
//				StatementWrapper prepareStatement = c.prepareStatement(SELECT8);)
//		{
//			prepareStatement.setInt(1, id);
//			try (ResultSet executeQuery = prepareStatement.executeQuery();)
//			{
//				if (executeQuery.next())
//				{
//					RootDirectory local = (RootDirectory) getAllocator(executeQuery).allocate(executeQuery);
//					local.fill(c, executeQuery, new DbLocals());
//					return local;
//				}
//			}
//			return null;
//		}
//		catch (SQLException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to get local by path", e);
//			return null;
//		}
//	}

	public static LocalDirectory getLocal(Path path) throws IOException
	{
		int pathId = DbRootPaths.getRootPath(path);
		if (pathId < 0)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get local by path");
			return null;
		}
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper prepareStatement = c.prepareStatement(SELECT5);)
		{
			prepareStatement.setLong(1, pathId);
			try (ResultSet executeQuery = prepareStatement.executeQuery();)
			{
				if (executeQuery.next())
				{
					return (LocalDirectory) DbObjects.ROOT.create(c, executeQuery);
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
				if (!executeQuery.next())
				{
					return null;
				}
				return (RootDirectory) DbObjects.ROOT.create(c, executeQuery, new DbLocals().setObject(machine));
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get root by name", e);
			return null;
		}
	}
	

	private static final QueryWrapper DELETE3_1 = new QueryWrapper("delete from SFILE where ROOT=? "
			+ "limit 50"
			+ ";");
	private static final QueryWrapper DELETE3_2 = new QueryWrapper(
			"delete from ROOT_CONTAINS r1 where RID=? "
			+ "and not exists (select RC_ID from ROOT_CONTAINS r2 where r2.PARENT=r1.RC_ID limit 1) "
			+ "limit 50 "
			+ ";");
	private static final QueryWrapper COUNT_PATHS_TO_DELETE = new QueryWrapper(
			"select count(RC_ID) from ROOT_CONTAINS where RID=?;");
	private static final QueryWrapper COUNT_FILES_TO_DELETE = new QueryWrapper(
			"select count(F_ID) from SFILE where ROOT=?;");

	private static final QueryWrapper COUNT_PATHS_TO_CLEAN = new QueryWrapper(
			"select count(P_ID) from PELEM where not exists (select RC_ID from ROOT_CONTAINS where ROOT_CONTAINS.PELEM=PELEM.P_ID limit 1);");
	private static final QueryWrapper REMOVE_PATHS = new QueryWrapper(
			"delete from PELEM where not exists (select RC_ID from ROOT_CONTAINS where ROOT_CONTAINS.PELEM=PELEM.P_ID limit 1) limit 500;");
	
	
	
	
	
	public static void deleteRoot(RootDirectory root, boolean completely)
	{
		int pathId = DbRootPaths.getRootPath(root.getPath());
		if (pathId < 0)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete root");
			return;
		}
		
		DeleteRootProgress progressBar = new DeleteRootProgress(new String[]
    {
        "Deleting files",
        "Deleting paths",
        "Deleting root path",
        "Deleting root info",
        "Cleaning strings",
    });
		Services.notifications.registerWindow(progressBar);
		progressBar.setVisible(true);
		progressBar.setAlwaysOnTop(true);
		
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				 StatementWrapper s1 = c.prepareStatement(DELETE3_1);
				 StatementWrapper s2 = c.prepareStatement(DELETE3_2);
				 StatementWrapper s3 = c.prepareStatement(DELETE3);
				 StatementWrapper s4 = c.prepareStatement(REMOVE_PATHS);
				 StatementWrapper countFiles = c.prepareStatement(COUNT_FILES_TO_DELETE);
				 StatementWrapper countPaths = c.prepareStatement(COUNT_PATHS_TO_DELETE);
				 StatementWrapper countClean = c.prepareStatement(COUNT_PATHS_TO_CLEAN);)
		{
			int numRemoved = -1;
			int totalFiles = -1;
			int totalPaths = -1;
			int totalClean = -1;
			
			countFiles.setInt(1, root.getId());
			try (ResultSet executeQuery = countFiles.executeQuery();)
			{
				if (executeQuery.next())
				{
					totalFiles = executeQuery.getInt(1);
				}
			}
			
			progressBar.begin(0, totalFiles);
			do
			{
				s1.setInt(1, root.getId());
				numRemoved = s1.executeUpdate();
				progressBar.updateProgress(numRemoved);
				LogWrapper.getLogger().fine("Removed " + numRemoved + " files: " + (totalFiles -= numRemoved) + " left.");
			} while (numRemoved > 0);

			countPaths.setInt(1, root.getId());
			try (ResultSet executeQuery = countPaths.executeQuery();)
			{
				if (executeQuery.next())
				{
					totalPaths = executeQuery.getInt(1);
				}
			}

			progressBar.begin(1, totalPaths);
			do
			{
				s2.setInt(1, root.getId());
				numRemoved = s2.executeUpdate();
				progressBar.updateProgress(numRemoved);
				LogWrapper.getLogger().fine("Removed " + numRemoved + " paths: " + (totalPaths -= numRemoved) + " left.");
			} while (numRemoved > 0);


			if (completely)
			{
				progressBar.begin(2, 1);
				s3.setInt(1, root.getId());
				s3.execute();
				LogWrapper.getLogger().fine("Done removing " + root);
				
				progressBar.begin(3, 1);
				DbRootPaths.removeRootPath(pathId);
				LogWrapper.getLogger().fine("Done removing root path. ");
			}

			Services.notifications.localsChanged();

			try (ResultSet executeQuery = countClean.executeQuery();)
			{
				if (executeQuery.next())
				{
					totalClean = executeQuery.getInt(1);
				}
			}
			progressBar.begin(4, totalClean);
			do
			{
				numRemoved = s4.executeUpdate();
				progressBar.updateProgress(numRemoved);
				LogWrapper.getLogger().fine("Cleaned " + numRemoved + " paths: " + (totalClean -= numRemoved) + " left.");
			} while (numRemoved > 0);

			LogWrapper.getLogger().fine("Done cleaning pelems.");
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete root", e);
		}
		finally
		{
			progressBar.dispose();
		}
	}
        
	
	public static class IgnorePatterns
	{
		private final String[] patterns;
		long minFileSize = -1;
		long maxFileSize = -1;
		private int permissionFlags;
		
		public static final int SKIP_HIDDEN       = 0x00000001;
		public static final int SKIP_EXECUTABLE   = 0x00000010;
		public static final int SKIP_NON_WRITABLE = 0x00000100;
		
		IgnorePatterns(String[] patterns, LocalDirectory local)
		{
			this.patterns = patterns;
			minFileSize = local.getMinFileSize();
			maxFileSize = local.getMaxFileSize();
			permissionFlags = local.getPermissionFlags();
		}
		
		public String[] getPatterns()
		{
			return this.patterns;
		}

		public static int createFlags(
				boolean skipHidden,
				boolean skipExe,
				boolean skipRO)
		{
			int returnValue = 0;
			if (skipHidden)
			{
				returnValue |= SKIP_HIDDEN;
			}
			if (skipExe)
			{
				returnValue |= SKIP_EXECUTABLE;
			}
			if (skipRO)
			{
				returnValue |= SKIP_NON_WRITABLE;
			}
			return returnValue;
		}

		public static boolean isSkipHidden(int flags)
		{
			return (flags & SKIP_HIDDEN) != 0;
		}

		public static boolean isSkipExecutable(int flags)
		{
			return (flags & SKIP_EXECUTABLE) != 0;
		}

		public static boolean isSkipRO(int flags)
		{
			return (flags & SKIP_NON_WRITABLE) != 0;
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
				if (!Files.isReadable(path))
				{
					return true;
				}
				if ((permissionFlags & SKIP_HIDDEN) != 0)
				{
					if (Files.isHidden(path))
					{
						return true;
					}
				}
				if ((permissionFlags & SKIP_EXECUTABLE) != 0)
				{
					if (Files.isExecutable(path))
					{
						return true;
					}
				}
				if ((permissionFlags & SKIP_NON_WRITABLE) != 0)
				{
					if (!Files.isWritable(path))
					{
						return true;
					}
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
		return new IgnorePatterns(returnValue.toArray(DUMMY), local);
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
					LogWrapper.getLogger().info("Unable to add ignore because it is the empty string.");
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
