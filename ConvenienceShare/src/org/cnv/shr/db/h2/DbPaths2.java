package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Objects;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class DbPaths2
{
	public static PathElement ROOT = new PathElement((long) 0, null, null, "", false, 0)
	{
		@Override
		public PathElement getParent()
		{
			return ROOT;
		}
		@Override
		public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException {}
	};
	
	
	private static final QueryWrapper GET_PATH_ID         = new QueryWrapper("select P_ID from PELEM where PELEM = ?;");
	private static final QueryWrapper INSERT_PATH_ID      = new QueryWrapper("merge into PELEM key(PELEM) values(DEFAULT, ?, ?);");
	private static final QueryWrapper MERGE_ROOT_CONTAINS = new QueryWrapper("merge into ROOT_CONTAINS key(PELEM, RID, PARENT) values(DEFAULT, ?, ?, ?);");
	private static final QueryWrapper FIND_ROOT_CONTAINS  = new QueryWrapper("select RC_ID from ROOT_CONTAINS where RID=? and PELEM=? and PARENT=?;");

	private static final QueryWrapper DELETE_ROOT_CONTAINS  = new QueryWrapper("delete from ROOT_CONTAINS where RC_ID=?;");
	private static final QueryWrapper COUNT_ROOT_CONTAINS   = new QueryWrapper("select count(RC_ID) from ROOT_CONTAINS where PARENT=?;");
	private static final QueryWrapper DELETE_PATH           = new QueryWrapper("delete from PELEM where P_ID=?;");
	private static final QueryWrapper COUNT_CHILD_PATH      = new QueryWrapper("select count(RC_ID) from ROOT_CONTAINS where PELEM=?;");
	
	private static final QueryWrapper LIST                  = new QueryWrapper(
			"select RC_ID, P_ID, BROKEN, PELEM.PELEM from ROOT_CONTAINS "
			+ "join PELEM on ROOT_CONTAINS.PELEM = PELEM.P_ID "
			+ "where PARENT=?;");

	private static final QueryWrapper COUNT_PATHS           = new QueryWrapper("select count(P_ID) from PELEM;");
	private static final QueryWrapper COUNT_CONTAINS        = new QueryWrapper("select count(RC_ID) from ROOT_CONTAINS;");
	

	private static final QueryWrapper CLEAN_PELEM           = new QueryWrapper("delete from PELEM where not exists (select count(RC_ID) from ROOT_CONTAINS where ROOT_CONTAINS.PELEM=PELEM.P_ID);");
	
//	private static final QueryWrapper GET_PATH              = new QueryWrapper(
//			"select RID, P_ID, BROKEN, PELEM.PELEM from ROOT_CONTAINS "
//			+ "join PELEM on ROOT_CONTAINS.PELEM = PELEM.P_ID "
//			+ "where RC_ID=?;");
	private static final QueryWrapper GET_PATH              = new QueryWrapper(
			"select RID, PELEM, PARENT from ROOT_CONTAINS where RC_ID=?;");
	private static final QueryWrapper GET_PELEM_VALUE       = new QueryWrapper(
			"select PELEM, BROKEN from PELEM where P_ID=?;");

	public static PathElement getPath(long id)
	{
		return getPath(id, null);
	}
	
	public static PathElement getPath(long id, DbLocals locals)
	{
		if (locals == null)
		{
			locals = new DbLocals();
		}
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper getPath  = wrapper.prepareStatement(GET_PATH);
				 StatementWrapper getValue = wrapper.prepareStatement(GET_PELEM_VALUE);)
		{
			return getPath(wrapper, getPath, getValue, id, null, locals);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read root path.", e);
			return null;
		}
	}
	
	private static PathElement getPath(
			ConnectionWrapper wrapper, 
			StatementWrapper getPath, 
			StatementWrapper getValue, 
			long id,
			RootDirectory root,
			DbLocals locals) throws SQLException
	{
		if (id == 0)
		{
			return ROOT;
		}

		int rootId;
		long pathId;
		boolean broken;
		long parentId;
		String value;
		
		getPath.setLong(1, id);
		try (ResultSet executeQuery = getPath.executeQuery();)
		{
			if (!executeQuery.next())
			{
				throw new RuntimeException("Unable to find the root contains.");
			}
			int ndx = 1;
			rootId = executeQuery.getInt(ndx++);
			pathId = executeQuery.getLong(ndx++);
			parentId = executeQuery.getLong(ndx++);
		}

		getValue.setLong(1, pathId);
		try (ResultSet executeQuery = getValue.executeQuery();)
		{
			if (!executeQuery.next())
			{
				throw new RuntimeException("Unable to find pelem.");
			}
			int ndx = 1;
			value = executeQuery.getString(ndx++);
			broken = executeQuery.getBoolean(ndx++);
		}
		
		if (root == null)
		{
			root = (RootDirectory) DbObjects.ROOT.find(wrapper, rootId, locals);
		}
		
		PathElement parent = getPath(wrapper, getPath, getValue, parentId, root, locals);
		if (parent == null)
		{
			return null;
		}
		
		return new PathElement(id, root, parent, value, broken, pathId);
	}

	static int getNumPaths() throws SQLException
	{
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper query = wrapper.prepareStatement(COUNT_PATHS);
				 ResultSet results = query.executeQuery();)
		{
			if (!results.next())
			{
				throw new RuntimeException("Unable to execute query.");
			}
			return results.getInt(1);
		}
	}
	static int getNumContains() throws SQLException
	{
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();
				 StatementWrapper query = wrapper.prepareStatement(COUNT_CONTAINS);
				 ResultSet results = query.executeQuery();)
		{
			if (!results.next())
			{
				throw new RuntimeException("Unable to execute query.");
			}
			return results.getInt(1);
		}
	}

	public static LinkedList<PathElement> listPaths(RootDirectory root)
	{
		return listPaths(root, ROOT);
	}
	public static LinkedList<PathElement> listPaths(PathElement element)
	{
		Objects.requireNonNull(element.getRoot());
		return listPaths(element.getRoot(), element);
	}
	
	public static LinkedList<PathElement> listPaths(RootDirectory root, PathElement element)
	{
		LinkedList<PathElement> returnValue = new LinkedList<>();
		
		try (ConnectionWrapper wrapper     = Services.h2DbCache.getThreadConnection();
				 StatementWrapper statement    = wrapper.prepareStatement(LIST);)
		{
			statement.setLong(1, element.getId());
			
			try (ResultSet results = statement.executeQuery())
			{
				while (results.next())
				{
					int ndx = 1;
					long containsId = results.getLong(ndx++);
					long pathId = results.getLong(ndx++);
					boolean broken = results.getBoolean(ndx++);
					String value = results.getString(ndx++);
					
					returnValue.add(new PathElement(containsId, root, element, value, broken, pathId));
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list paths", e);
		}
		
		return returnValue;
	}

	public static PathElement addDirectoryPath(RootDirectory root, String fullPath)
	{
		return addPathTo(root, ROOT, fullPath, true);
	}
	
	public static PathElement addFilePath(RootDirectory root, String fullPath)
	{
		return addPathTo(root, ROOT, fullPath, false);
	}
	
	public static PathElement addPathTo(RootDirectory root, PathElement parent, String value, boolean directory)
	{
		PathBreakInfo[] elements = PathBreaker.breakThePath(value, directory);
		int rootId = root.getId();

		Services.h2DbCache.setAutoCommit(false);
		try (ConnectionWrapper wrapper     = Services.h2DbCache.getThreadConnection();
				 StatementWrapper getPathId    = wrapper.prepareStatement(GET_PATH_ID);
				 StatementWrapper insertPathId = wrapper.prepareStatement(INSERT_PATH_ID, Statement.RETURN_GENERATED_KEYS);
				 StatementWrapper merge        = wrapper.prepareStatement(MERGE_ROOT_CONTAINS, Statement.RETURN_GENERATED_KEYS);
				 StatementWrapper find         = wrapper.prepareStatement(FIND_ROOT_CONTAINS))
		{
			for (int i=0; i<elements.length; i++)
			{
				PathBreakInfo info = elements[i];
				long pathId = -1;
				
				getPathId.setString(1, info.element);
				try (ResultSet results = getPathId.executeQuery();)
				{
					if (results.next())
					{
						pathId = results.getLong(1);
					}
				}
				if (pathId == -1)
				{
					int ndx = 1;
					insertPathId.setBoolean(ndx++, info.broken);
					insertPathId.setString(ndx++, info.element);
					insertPathId.executeUpdate();
					try (ResultSet results = insertPathId.getGeneratedKeys())
					{
						if (!results.next())
						{
							throw new RuntimeException("Should not get here...");
						}
						pathId = results.getLong(1);
					}
				}
				
				int ndx;
				
				ndx = 1;
				find.setInt(ndx++, rootId);
				find.setLong(ndx++, pathId);
				find.setLong(ndx++, parent.getId());
				try (ResultSet results = find.executeQuery())
				{
					if (results.next())
					{
						parent = new PathElement(results.getLong(1), root, parent, info.element, info.broken, pathId);
						wrapper.commit();
						continue;
					}
				}

				ndx = 1;
				merge.setInt(ndx++, rootId);
				merge.setLong(ndx++, pathId);
				merge.setLong(ndx++, parent.getId()); 
				merge.executeUpdate();
				try (ResultSet results = merge.getGeneratedKeys())
				{
					if (!results.next())
					{
						throw new RuntimeException("Unable to create new ROOT_CONTAINS row...");
					}
					parent = new PathElement(results.getLong(1), root, parent, info.element, info.broken, pathId);
				}
				wrapper.commit();
			}
			
			return parent;
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to make a root contains...", e);
			return null;
		}
		finally
		{
			Services.h2DbCache.setAutoCommit(true);
		}
	}

	public static PathElement findFilePath(RootDirectory root, String fullPath)
	{
		return findPathIn(root, ROOT, fullPath, false);
	}

	public static PathElement findDirectoryPath(RootDirectory root, String fullPath)
	{
		return findPathIn(root, ROOT, fullPath, true);
	}

	public static PathElement findPathIn(RootDirectory root, PathElement parent, String value, boolean directory)
	{
		PathBreakInfo[] elements = PathBreaker.breakThePath(value, directory);
		int rootId = root.getId();

		try (ConnectionWrapper wrapper     = Services.h2DbCache.getThreadConnection();
				 StatementWrapper getPathId    = wrapper.prepareStatement(GET_PATH_ID);
				 StatementWrapper find         = wrapper.prepareStatement(FIND_ROOT_CONTAINS))
		{
			for (int i=0; i<elements.length; i++)
			{
				PathBreakInfo info = elements[i];
				long pathId = -1;
				
				getPathId.setString(1, info.element);
				try (ResultSet results = getPathId.executeQuery();)
				{
					if (results.next())
					{
						pathId = results.getLong(1);
					}
					else
					{
						return null;
					}
				}
				
				int ndx;
				
				ndx = 1;
				find.setInt(ndx++, rootId);
				find.setLong(ndx++, pathId);
				find.setLong(ndx++, parent.getId());
				try (ResultSet results = find.executeQuery())
				{
					if (!results.next())
					{
						return null;
					}
					parent = new PathElement(results.getLong(1), root, parent, info.element, info.broken, pathId);
				}
			}
			
			return parent;
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to make a root contains...", e);
			return null;
		}
	}
	
	

	public static void removePathFromRoot(PathElement element)
	{
		Services.h2DbCache.setAutoCommit(false);
		try (ConnectionWrapper wrapper  = Services.h2DbCache.getThreadConnection();
				 StatementWrapper rmRoot    = wrapper.prepareStatement(DELETE_ROOT_CONTAINS);
				 StatementWrapper countRoot = wrapper.prepareStatement(COUNT_ROOT_CONTAINS);
				 StatementWrapper rmPath    = wrapper.prepareStatement(DELETE_PATH);
				 StatementWrapper countPath = wrapper.prepareStatement(COUNT_CHILD_PATH))
		{
			boolean shouldContinue;
			do
			{
				shouldContinue = false;
				
				rmRoot.setLong(1, element.getId());
				rmRoot.execute();
				
				// Does not remove the children of this path that are no longer used...
				
				countPath.setLong(1, element.getPathId());
				try (ResultSet executeQuery = countPath.executeQuery();)
				{
					if (!executeQuery.next())
					{
						throw new RuntimeException("Unable to count the number of root contains for path.");
					}
					if (executeQuery.getInt(1) <= 0)
					{
						rmPath.setLong(1, element.getPathId());
						rmPath.execute();
					}
				}
				
				if (!element.isBroken())
				{
					break;
				}
				
				element = element.getParent();
				if (element.getId() == 0)
				{
					break;
				}
				
				countRoot.setLong(1, element.getId());
				try (ResultSet executeQuery = countRoot.executeQuery();)
				{
					if (!executeQuery.next())
					{
						throw new RuntimeException("Unable to count the number of children root contains for path.");
					}
					if (executeQuery.getInt(1) > 0)
					{
						break;
					}
				}
				
				wrapper.commit();
			} while (shouldContinue);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove root contains...", e);
		}
		finally
		{
			Services.h2DbCache.setAutoCommit(true);
		}
	}
	
	public static void cleanPelem()
	{
		try (ConnectionWrapper wrapper  = Services.h2DbCache.getThreadConnection();
				 StatementWrapper countPath = wrapper.prepareStatement(CLEAN_PELEM);)
		{
			countPath.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to clean pelem.", e);
		}
	}

	public static final class PathBreakInfo
	{
		private String element;
		private boolean broken;
		
		public PathBreakInfo(String element, boolean broken)
		{
			this.element = element;
			this.broken  = broken;
		}
	}
}
