package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

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
			Services.logger.println("Unable to get file size of " + d);
			Services.logger.print(e);
			return -1;
		}
	}

	public static long getNumberOfFiles(RootDirectory d)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
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
			Services.logger.println("Unable to count files in " + d);
			Services.logger.print(e);
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
			Services.logger.print(e);
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
			Services.logger.print(e);
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
			ResultSet executeQuery = prepareStatement.executeQuery();
			if (executeQuery.next())
			{
				LocalDirectory local = (LocalDirectory) DbTables.DbObjects.LROOT.allocate(executeQuery);
				local.fill(c, executeQuery, new DbLocals().setObject(Services.localMachine).setObject(pathElement));
				return local;
			}
			return null;
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
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
			ResultSet executeQuery = prepareStatement.executeQuery();

			DbObjects o = machine.isLocal() ? DbTables.DbObjects.LROOT : DbTables.DbObjects.RROOT;
			
			if (!executeQuery.next())
			{
				return null;
			}
			DbObject allocate = o.allocate(executeQuery);
			allocate.fill(c, executeQuery, new DbLocals().setObject(machine));
			return (RootDirectory) allocate;
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
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
			Services.logger.print(e);
		}
		DbPaths.removeUnusedPaths();
	}
        
	
	public static class IgnorePatterns
	{
		private final String[] patterns;
		
		IgnorePatterns(String[] patterns)
		{
			this.patterns = patterns;
		}
		
		public String[] getPatterns()
		{
			return this.patterns;
		}
		
		public boolean blocks(String path)
		{
			for (String pattern : patterns)
			{
				if (path.contains(pattern))
				{
					return true;
				}
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
			ResultSet results = s1.executeQuery();
			while (results.next())
			{
				returnValue.add(results.getString(1));
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		return new IgnorePatterns(returnValue.toArray(DUMMY));
	}

        public static void setIgnores(LocalDirectory local, String[] ignores)
        {
            try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
            		StatementWrapper s1 = c.prepareStatement(DELETE4);) {
                s1.setInt(1, local.getId());
                s1.execute();
            } catch (SQLException ex) {
                Services.logger.println(ex);
            }
            HashSet<String> ignoresAdded = new HashSet<>();
            try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
            		StatementWrapper s1 = c.prepareStatement(INSERT1);) {
                for (String ignore : ignores)
                {
                    ignore = ignore.trim();
                    if (ignore.length() > 1024)
                    {
                        Services.logger.println("Unable to add ignore because it is bigger than the maximum ignore pattern: " + ignore);
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
            } catch (SQLException ex) {
                Services.logger.println(ex);
            }
        }
}
