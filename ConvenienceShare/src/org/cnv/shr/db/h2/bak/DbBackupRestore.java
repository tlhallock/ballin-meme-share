package org.cnv.shr.db.h2.bak;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.DbRestoreProgress;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.LogWrapper;

public class DbBackupRestore
{
	private static final QueryWrapper ALL_LOCALS = new QueryWrapper(
			"select IS_SHARING, RNAME, IDENT from SHARE_ROOT "
			+ "join ROOT on RID=R_ID "
			+ "join MACHINE on SHARE_ROOT.MID=M_ID "
			+ "where ROOT.IS_LOCAL=true;");
	
	public static void backupDatabase(File f) throws IOException
	{
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(Paths.get(f.getAbsolutePath())));)
		{
			generator.writeStartObject();
			generator.writeStartArray("machines");
			try (DbIterator<Machine> listLocals = DbMachines.listRemoteMachines();)
			{
				while (listLocals.hasNext())
				{
					new MachineBackup(listLocals.next()).generate(generator);
				}
			}
			generator.writeEnd();
			
			Services.notifications.remotesChanged();

			generator.writeStartArray("directories");
			try (DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();)
			{
				while (listLocals.hasNext())
				{
					new LocalBackup(listLocals.next()).generate(generator);
				}
			}
			generator.writeEnd();
			Services.notifications.localsChanged();

			generator.writeStartArray("files");
			try (DbIterator<LocalFile> listLocals = DbFiles.listAllLocalFiles())
			{
				while (listLocals.hasNext())
				{
					new FileBackup(listLocals.next()).generate(generator);
				}
			}
			generator.writeEnd();
			
			Services.notifications.localsChanged();

			generator.writeStartArray("permissions");
			try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection(); 
					StatementWrapper stmt = wrapper.prepareStatement(ALL_LOCALS);
					ResultSet results = stmt.executeQuery())
			{
				while (results.next())
				{
					SharingState state = SharingState.get(results.getInt(1));
					LocalDirectory local = DbRoots.getLocalByName(results.getString(2));
					Machine machine = DbMachines.getMachine(results.getString(3));
					
					new RootPermissionBackup(local, machine, state).generate(generator);
				}
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to list permissions.", e);
			}
			generator.writeEnd();

			generator.writeEnd();
			Services.notifications.localsChanged();
		}
	}

	public static void restoreDatabase(File f)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					restoreDatabaseInternal(f);
				}
				catch (IOException | SQLException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to restore database.", e);
				}
			}
		});
	}
	
	private synchronized static void restoreDatabaseInternal(File f) throws IOException, SQLException
	{
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();)
		{
			DbTables.deleteDb(wrapper);
		}
		DbRestoreProgress dbRestoreProgress = new DbRestoreProgress(f.length());
		dbRestoreProgress.setVisible(true);
		
		try (
				CountingInputStream newInputStream = new CountingInputStream(Files.newInputStream(Paths.get(f.getAbsolutePath())));
				JsonParser parser = TrackObjectUtils.createParser(newInputStream);
				ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();)
		{
			String key = null;
			while (parser.hasNext())
			{
				Event next = parser.next();
				switch (next)
				{
				case KEY_NAME:
					key = parser.getString();
					break;
				case START_ARRAY:
					switch (key)
					{
					case "machines":
						dbRestoreProgress.setState("Reading machines");
						readMachines(parser, wrapper, dbRestoreProgress, newInputStream);
						break;
					case "directories":
						dbRestoreProgress.setState("Reading directories");
						readDirectories(parser, wrapper, dbRestoreProgress, newInputStream);
						break;
					case "files":
						dbRestoreProgress.setState("Reading files");
						readFiles(parser, wrapper, dbRestoreProgress, newInputStream);
						break;
					case "permissions":
						dbRestoreProgress.setState("Reading permissions");
						readPermissions(parser, wrapper, dbRestoreProgress, newInputStream);
						break;
					}
				}
			}
		}
		finally
		{
			dbRestoreProgress.done();
		}
	}

	private static void readFiles(JsonParser parser, 
			ConnectionWrapper wrapper,
			DbRestoreProgress p, 
			CountingInputStream newInputStream)
	{
		Event next;
		while (parser.hasNext())
		{
			next = parser.next();
			switch (next)
			{
			case END_ARRAY:
				return;
			case START_OBJECT:
				new FileBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}

	private static void readMachines(JsonParser parser, 
			ConnectionWrapper wrapper,
			DbRestoreProgress p,
			CountingInputStream newInputStream)
	{
		Event next;
		while (parser.hasNext())
		{
			next = parser.next();
			switch (next)
			{
			case END_ARRAY:
				return;
			case START_OBJECT:
				new MachineBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}

	private static void readDirectories(JsonParser parser,
			ConnectionWrapper wrapper, 
			DbRestoreProgress p, CountingInputStream newInputStream)
	{
		Event next;
		while (parser.hasNext())
		{
			next = parser.next();
			switch (next)
			{
			case END_ARRAY:
				return;
			case START_OBJECT:
				new LocalBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}

	private static void readDownloads(JsonParser parser,
			ConnectionWrapper wrapper, 
			DbRestoreProgress p, CountingInputStream newInputStream)
	{
		Event next;
		while (parser.hasNext())
		{
			next = parser.next();
			switch (next)
			{
			case END_ARRAY:
				return;
			case START_OBJECT:
				new LocalBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}

	private static void readMessages(JsonParser parser,
			ConnectionWrapper wrapper, 
			DbRestoreProgress p, CountingInputStream newInputStream)
	{
		Event next;
		while (parser.hasNext())
		{
			next = parser.next();
			switch (next)
			{
			case END_ARRAY:
				return;
			case START_OBJECT:
				new LocalBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}

	private static void readPermissions(JsonParser parser, 
			ConnectionWrapper wrapper,
			DbRestoreProgress p,
			CountingInputStream newInputStream)
	{
		Event next;
		while (parser.hasNext())
		{
			next = parser.next();
			switch (next)
			{
			case END_ARRAY:
				return;
			case START_OBJECT:
				new RootPermissionBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}
}
