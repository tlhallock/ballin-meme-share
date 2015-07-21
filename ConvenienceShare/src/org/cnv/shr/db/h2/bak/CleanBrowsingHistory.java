package org.cnv.shr.db.h2.bak;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;

public class CleanBrowsingHistory
{
	private static final QueryWrapper SELECT = new QueryWrapper(
			"select * from DOWNLOAD "
			+ "join SFILE on DOWNLOAD.FID=SFILE.F_ID "
			+ "join ROOT on SFILE.ROOT=ROOT.R_ID "
			+ "where ROOT.MID = ?;");

//	private static final QueryWrapper SELECT_ROOT_PATHS = new QueryWrapper("select PELEM from ROOT where MID=?;");
	
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
		DbPaths2.cleanPelem();
	}

	private static void add(LinkedList<DownloadBackup> extracted)
	{
		for (DownloadBackup backup : extracted)
		{
			try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();)
			{
				backup.save(connection);
			}
			catch (SQLException | IOException e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to list download " + backup, e1);
			}
		}
	}

	private static void cleanMachine(Machine machine)
	{
		try (DbIterator<RootDirectory> list = DbRoots.list(machine);)
		{
			while (list.hasNext())
			{
				DbRoots.deleteRoot(list.next(), false);
			}
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
