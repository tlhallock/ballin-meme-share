package org.cnv.shr.db.h2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class DbPaths2
{
	public static PathElement getRoot(RootDirectory root)
	{
		return new PathElement((long) 0, root, null, "/", false, 0)
		{
			@Override
			public PathElement getParent()
			{
				return this;
			}

			public String getFullPath()
			{
				return "/";
			}

			public boolean isRoot()
			{
				return true;
			}
			
			public RootDirectory getRoot() { return root; }
		};
	}
	
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
			+ "where RID = ? "
			+ "and RC_ID <> 0 "
			+ "and PARENT=?;");

	private static final QueryWrapper COUNT_PATHS           = new QueryWrapper("select count(P_ID) from PELEM;");
	private static final QueryWrapper COUNT_CONTAINS        = new QueryWrapper("select count(RC_ID) from ROOT_CONTAINS;");
	

	private static final QueryWrapper CLEAN_PELEM           = new QueryWrapper("delete from PELEM where not exists (select RC_ID from ROOT_CONTAINS where ROOT_CONTAINS.PELEM=PELEM.P_ID limit 1);");
	
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
			return getRoot(root);
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
		return listPaths(getRoot(root));
	}
	public static LinkedList<PathElement> listPaths(PathElement element)
	{
		Objects.requireNonNull(element.getRoot());
		LinkedList<PathElement> returnValue = new LinkedList<>();
		
		try (ConnectionWrapper wrapper     = Services.h2DbCache.getThreadConnection();
				StatementWrapper statement    = wrapper.prepareStatement(LIST);)
		{
			statement.setInt(1, element.getRoot().getId());
			statement.setLong(2, element.getId());
			
			try (ResultSet results = statement.executeQuery())
			{
				while (results.next())
				{
					int ndx = 1;
					long containsId = results.getLong(ndx++);
					long pathId = results.getLong(ndx++);
					boolean broken = results.getBoolean(ndx++);
					String value = results.getString(ndx++);
					
					returnValue.add(new PathElement(containsId, element.getRoot(), element, value, broken, pathId));
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
		return addPathTo(root, getRoot(root), fullPath, true);
	}
	
	public static PathElement addFilePath(RootDirectory root, String fullPath)
	{
		return addPathTo(root, getRoot(root), fullPath, false);
	}
	
	public static PathElement addPathTo(RootDirectory root, PathElement parent, String value, boolean directory)
	{
		if (value.equals("/"))
		{
			return getRoot(root);
		}
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
				
				if (pathId == 0)
				{
					// This is the root...
					continue;
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
		return findPathIn(root, getRoot(root), fullPath, false);
	}

	public static PathElement findDirectoryPath(RootDirectory root, String fullPath)
	{
		if (fullPath.equals("/"))
		{
			return getRoot(root);
		}
		return findPathIn(root, getRoot(root), fullPath, true);
	}

	public static PathElement findPathIn(RootDirectory root, PathElement parent, String value, boolean directory)
	{
		PathBreakInfo[] elements = PathBreaker.breakThePath(value, directory);
		int rootId = root.getId();

		try (ConnectionWrapper wrapper     = Services.h2DbCache.getThreadConnection();
				 StatementWrapper getPathId    = wrapper.prepareStatement(GET_PATH_ID);
				 StatementWrapper find         = wrapper.prepareStatement(FIND_ROOT_CONTAINS))
		{
			// skip the root element...
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
				
				if (pathId == 0)
				{
					// This is the root...
					continue;
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
		if (element.isRoot())
		{
			LogWrapper.getLogger().info("Cannot delete the root path.");
			return;
		}
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
				if (element.isRoot())
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

//private static void generateRoots(
//		ConnectionWrapper connection, 
//		long childId, 
//		HashMap<Integer, String> rootNames, 
//		JsonGenerator generator)
//		throws SQLException
//{
//	generator.writeStartArray("roots");
//	try (StatementWrapper rootStmt = connection.prepareNewStatement(
//			"select RID from ROOT_CONTAINS where PELEM = " + childId + ";");
//			ResultSet rootResults = rootStmt.executeQuery();)
//	{
//		while (rootResults.next())
//		{
//			int rootId = rootResults.getInt(1);
//			String string = rootNames.get(rootId);
//			if (string == null)
//			{
//				try (StatementWrapper nameStmt = connection.prepareNewStatement(
//						"select MNAME, RNAME from ROOT join MACHINE on MID=M_ID where R_ID=" + rootId + " limit 1;");
//						ResultSet rootName = nameStmt.executeQuery();)
//				{
//					if (rootName.next())
//						string = rootName.getString(1) + ":" + rootName.getString(2);
//					else
//						string = "Unkown root";
//				}
//				rootNames.put(rootId, string);
//			}
//			generator.write(string);
//		}
//	}
//	generator.writeEnd();
//}
	

//	private static final QueryWrapper LIST_CONTAINS_CHILDREN = new QueryWrapper("select "
//		+ "ROOT_CONTAINS.RC_ID, PELEM.BROKEN, PELEM.PELEM "
//			+ "from ROOT_CONTAINS join PELEM on ROOT_CONTAINS.PELEM=PELEM.R_ID "
//			+ "where ROOT_CONTAINS.PARENT = ? "
//			+ "and RID=?;");

	private static final QueryWrapper LIST_CONTAINS_CHILDREN = new QueryWrapper("select "
		+ "ROOT_CONTAINS.RC_ID, ROOT_CONTAINS.PELEM "
			+ "from ROOT_CONTAINS "
			+ "where ROOT_CONTAINS.PARENT = ? "
			+ "and RID=?;");

	private static final QueryWrapper GET_PATH_INFO = new QueryWrapper("select "
		+ " PELEM.BROKEN, PELEM.PELEM "
			+ "from PELEM "
			+ "where PELEM.P_ID = ? limit 1;");

private static final QueryWrapper LIST_ALL_ROOTS = new QueryWrapper("select distinct RID from ROOT_CONTAINS;");

private static void debugChildPaths(
		ConnectionWrapper connection,
		JsonGenerator generator,
		long rcId,
		int rootId) 
				throws SQLException
{
	class TmpObject
	{
		long childId;
		boolean broken;
		String element;
	}
	LinkedList<TmpObject> toGenerate = new LinkedList<>();
	// Recursive, so cannot reuse statement... (Not true anymore)
	try (StatementWrapper prepareStatement = connection.prepareStatement(LIST_CONTAINS_CHILDREN);
			 StatementWrapper info = connection.prepareStatement(GET_PATH_INFO);)
	{
		prepareStatement.setLong(1, rcId);
		prepareStatement.setLong(2, rootId);
		try (ResultSet results = prepareStatement.executeQuery();)
		{
			while (results.next())
			{
				TmpObject o = new TmpObject();
				
				o.childId = results.getLong(1);
				if (o.childId == rcId)
				{
					if (rcId == 0)
					{
						continue;
					}
					throw new RuntimeException("Path is child of itself: " + o.childId);
				}
				
				info.setLong(1, results.getLong(2));
				try (ResultSet executeQuery = info.executeQuery();)
				{
					if (!executeQuery.next())
					{
						throw new RuntimeException("Can't follow pathId...");
					}
					o.broken = executeQuery.getBoolean(1);
					o.element = executeQuery.getString(2);
				}

				toGenerate.addLast(o);
			}
		}
	}

	generator.writeStartArray("children");
	for (TmpObject obj : toGenerate)
	{
		generator.writeStartObject();
		generator.write("id", obj.childId);
		generator.write("broken", obj.broken);
		generator.write("element", obj.element);
//		generateRoots(connection, obj.childId, rootNames, generator);
		debugChildPaths(connection, generator, obj.childId, rootId);
		generator.writeEnd();
	}
	generator.writeEnd();
}

private static HashMap<Integer, String> getRootNames(ConnectionWrapper connection) throws SQLException
{
	HashSet<Integer> rootIds = new HashSet<>();
	try (StatementWrapper prepareStatement = connection.prepareStatement(LIST_ALL_ROOTS); 
			 ResultSet executeQuery = prepareStatement.executeQuery();)
	{
		while (executeQuery.next())
		{
			rootIds.add(executeQuery.getInt(1));
		}
	}

	if (rootIds.isEmpty())
	{
		throw new RuntimeException("Did not find any roots in root contains");
	}
	StringBuilder nameBuilder = new StringBuilder();
	nameBuilder.append("select R_ID, MNAME, RNAME from ROOT join MACHINE on MID=M_ID where R_ID in (");
	Iterator<Integer> iterator = rootIds.iterator();
	nameBuilder.append(iterator.next());
	while (iterator.hasNext())
	{
		nameBuilder.append(", ").append(iterator.next());
	}
	nameBuilder.append(");");

	HashMap<Integer, String> rootNames = new HashMap<>();
	try (StatementWrapper nameStmt = connection.prepareNewStatement(nameBuilder.toString());
			 ResultSet names = nameStmt.executeQuery();)
	{
		while (names.next())
		{
			String value = names.getString(2) + ":" + names.getString(3);
			rootNames.put(names.getInt(1), value);
		}
	}
	return rootNames;
}

public static void debugPaths(Path p)
{
	LogWrapper.getLogger().info("Debugging the paths to " + p);
	try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
			JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(p), true);)
	{
		HashMap<Integer, String> rootNames = getRootNames(connection);
		
		generator.writeStartObject();
		generator.write("numPaths", DbPaths2.getNumPaths());
		generator.write("numContains", DbPaths2.getNumContains());
		generator.writeStartArray("roots");
		
		for (Map.Entry<Integer, String> rootId : rootNames.entrySet())
		{
			generator.writeStartObject();
			generator.write("rootName", rootId.getValue());
			generator.write("id", 0);
			generator.write("broken", false);
			generator.write("element", "/");
			debugChildPaths(connection, generator, 0, rootId.getKey());
			generator.writeEnd();
		}
		generator.writeEnd();
		generator.writeEnd();
	}
	catch (IOException | SQLException e)
	{
		LogWrapper.getLogger().log(Level.INFO, "Unable to debug paths to " + p, e);
	}
	LogWrapper.getLogger().info("Done debugging paths: " + p);
}
}
