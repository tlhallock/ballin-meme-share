package org.cnv.shr.db.h2.bak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class CleanBrowsingHistory
{
	private static final QueryWrapper SELECT = new QueryWrapper(
			"select * from DOWNLOAD "
			+ "join SFILE on DOWNLOAD.FID=SFILE.F_ID "
			+ "join ROOT on SFILE.ROOT=ROOT.R_ID "
			+ "where ROOT.MID = ?;");

	private static final QueryWrapper SELECT_ROOT_PATHS = new QueryWrapper("select PELEM from ROOT where MID=?;");
	
	public static void removeAllNonEssentialData(Machine machine)
	{
		if (machine.isLocal())
		{
			throw new RuntimeException("Cannot remove local data!!!");
		}
		LogWrapper.getLogger().info("Backing up downloads");
		LinkedList<DownloadBackup> extracted = collectDownloadBackups(machine);
		LogWrapper.getLogger().info("Removing root all paths from machine.");
		cleanMachine(machine);
		LogWrapper.getLogger().info("Adding removed downloads");
		add(extracted);
		LogWrapper.getLogger().info("Cleaning paths.");
		DbPaths.removeUnusedPaths();
	}

	private static void add(LinkedList<DownloadBackup> extracted)
	{
		for (DownloadBackup backup : extracted)
		{
			try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();)
			{
				backup.save(connection);
			}
			catch (SQLException e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to list download " + backup, e1);
			}
		}
	}

	private static void cleanMachine(Machine machine)
	{
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = connection.prepareNewStatement(
						"delete from ROOT_CONTAINS "
								+ " where RID in (select R_ID from ROOT where MID=" + machine.getId() + ") "
								+ " and PELEM not in (" + collectRootPaths(machine) + ");");)
		{
			stmt.execute();
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove all paths from machine.", e1);
		}
	}

	private static String collectRootPaths(Machine machine)
	{
		HashSet<Long> toKeep = new HashSet<>();
		DbLocals locals = new DbLocals();
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				StatementWrapper prepareStatement = connection.prepareStatement(SELECT_ROOT_PATHS);)
		{
			prepareStatement.setInt(1, machine.getId());
			try (ResultSet results = prepareStatement.executeQuery();)
			{
				while (results.next())
				{
					PathElement pathElement = DbPaths.getPathElement(results.getLong(1), locals);
					do
					{
						toKeep.add(pathElement.getId());
						pathElement = pathElement.getParent();
					} while (pathElement.getId() != 0);
				}
			}
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list downloads.", e1);
		}
		StringBuilder builder = new StringBuilder(5 * toKeep.size());
		builder.append("0");
		for (Long l : toKeep)
		{
			builder.append(", ");
			builder.append(l);
		}
		return builder.toString();
	}

	private static LinkedList<DownloadBackup> collectDownloadBackups(Machine machine)
	{
		LinkedList<DownloadBackup> backups = new LinkedList<>();
		
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				StatementWrapper prepareStatement = connection.prepareStatement(SELECT);)
		{
			prepareStatement.setInt(1, machine.getId());
			try (ResultSet results = prepareStatement.executeQuery();
					DbIterator<Download> downloads = new DbIterator<Download>(connection, results, DbObjects.PENDING_DOWNLOAD))
			{
				while (downloads.hasNext())
				{
					Download next = downloads.next();
					backups.add(new DownloadBackup(next));
					next.delete();
				}
			}
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list downloads.", e1);
		}
		return backups;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static void generateRoots(
			ConnectionWrapper connection, 
			long childId, 
			HashMap<Integer, String> rootNames, 
			JsonGenerator generator)
			throws SQLException
	{
		generator.writeStartArray("roots");
		try (StatementWrapper rootStmt = connection.prepareNewStatement(
				"select RID from ROOT_CONTAINS where PELEM = " + childId + ";");
				ResultSet rootResults = rootStmt.executeQuery();)
		{
			while (rootResults.next())
			{
				int rootId = rootResults.getInt(1);
				String string = rootNames.get(rootId);
				if (string == null)
				{
					try (StatementWrapper nameStmt = connection.prepareNewStatement(
							"select MNAME, RNAME from ROOT join MACHINE on MID=M_ID where R_ID=" + rootId + " limit 1;");
							ResultSet rootName = nameStmt.executeQuery();)
					{
						if (rootName.next())
							string = rootName.getString(1) + ":" + rootName.getString(2);
						else
							string = "Unkown root";
					}
					rootNames.put(rootId, string);
				}
				generator.write(string);
			}
		}
		generator.writeEnd();
	}

	private static final QueryWrapper LIST_CHILDREN = new QueryWrapper("select P_ID, BROKEN, PELEM from PELEM where PARENT = ?;");
	private static void debugChildPaths(
			ConnectionWrapper connection,
			HashMap<Integer, String> rootNames, 
			JsonGenerator generator,
			long pathId) 
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
		try (StatementWrapper prepareStatement = connection.prepareStatement(LIST_CHILDREN);)
		{
			prepareStatement.setLong(1, pathId);
			try (ResultSet results = prepareStatement.executeQuery();)
			{
				while (results.next())
				{
					TmpObject o = new TmpObject();
					
					int ndx = 1;
					o.childId = results.getLong(ndx++);
					o.broken = results.getBoolean(ndx++);
					o.element = results.getString(ndx++);

					if (o.childId == pathId)
					{
						if (pathId == 0)
						{
							continue;
						}
						throw new RuntimeException("Path is child of itself: " + o.childId);
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
			generateRoots(connection, obj.childId, rootNames, generator);
			debugChildPaths(connection, rootNames, generator, obj.childId);
			generator.writeEnd();
		}
		generator.writeEnd();
	}
	
	public static void debugPaths(Path p)
	{
		HashMap<Integer, String> rootNames = new HashMap<>();
		
		LogWrapper.getLogger().info("Debugging the paths to " + p);
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(p), true);)
		{
			generator.writeStartObject();
			generator.write("id", DbPaths2.ROOT.getId());
			generator.write("broken", DbPaths2.ROOT.isBroken());
			generator.write("element", DbPaths2.ROOT.getName());
			generateRoots(connection, DbPaths2.ROOT.getId(), rootNames, generator);
			debugChildPaths(connection, rootNames, generator, DbPaths2.ROOT.getId());
			generator.writeEnd();
		}
		catch (IOException | SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to debug paths to " + p, e);
		}
		LogWrapper.getLogger().info("Done debugging paths: " + p);
	}
}
