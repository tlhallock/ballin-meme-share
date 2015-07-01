package org.cnv.shr.db.h2.bak;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.LogWrapper;

public class CleanBrowsingHistory
{
	private static final QueryWrapper SELECT = new QueryWrapper(
			"select * from DOWNLOAD "
			+ "join SFILE on DOWNLOAD.FID=SFILE.F_ID "
			+ "join ROOT on SFILE.ROOT=ROOT.R_ID "
			+ "where ROOT.MID = ?;");
	private static final QueryWrapper DELETE = new QueryWrapper(
			"delete from ROOT_CONTAINS where RID in (select R_ID from ROOT where MID=?);");

	public static void removeAllNonEssentialData(Machine machine)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				removeAllNonEssentialDataInternal(machine);
			}
		});
	}

	private static void removeAllNonEssentialDataInternal(Machine machine)
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
		
		Services.userThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				LogWrapper.getLogger().info("Cleaning paths.");
				DbPaths.removeUnusedPaths();
			}});
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
				StatementWrapper stmt = connection.prepareStatement(DELETE);)
		{
			stmt.setInt(1, machine.getId());
			stmt.execute();
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove all paths from machine.", e1);
		}
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
}
