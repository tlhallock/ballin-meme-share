
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



package org.cnv.shr.db.h2.bak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.swing.JFrame;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.DbRestoreProgress;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.UserMessage;
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
	
	public static void backupDatabase(Path f) throws IOException
	{
		LogWrapper.getLogger().info("Backing up the database.");
		
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(f), true);)
		{
			generator.writeStartObject();
			LogWrapper.getLogger().info("Writing machines.");
			generator.writeStartArray("machines");
			try (DbIterator<Machine> listLocals = DbMachines.listRemoteMachines();)
			{
				while (listLocals.hasNext())
				{
					new MachineBackup(listLocals.next()).generate(generator, null);
				}
			}
			generator.writeEnd();

			LogWrapper.getLogger().info("Writing local directories.");
			generator.writeStartArray("directories");
			try (DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();)
			{
				while (listLocals.hasNext())
				{
					new LocalBackup(listLocals.next()).generate(generator, null);
				}
			}
			generator.writeEnd();

			LogWrapper.getLogger().info("Writing local files.");
			generator.writeStartArray("files");
			try (DbIterator<LocalFile> listLocals = DbFiles.listAllLocalFiles())
			{
				while (listLocals.hasNext())
				{
					new FileBackup(listLocals.next()).generate(generator, null);
				}
			}
			generator.writeEnd();
			
			LogWrapper.getLogger().info("Writing directory permissions.");
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
					
					new RootPermissionBackup(local, machine, state).generate(generator, null);
				}
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to list permissions.", e);
			}
			generator.writeEnd();
			

			LogWrapper.getLogger().info("Writing user messages.");
			generator.writeStartArray("messages");
			try (DbIterator<UserMessage> messages = DbMessages.listMessages())
			{
				while (messages.hasNext())
				{
					new MessageBackup(messages.next()).generate(generator, null);
				}
			}
			generator.writeEnd();
			

			LogWrapper.getLogger().info("Writing downloads.");
			generator.writeStartArray("downloads");
			try (DbIterator<Download> downloads = DbDownloads.listDownloads();)
			{
				while (downloads.hasNext())
				{
					new DownloadBackup(downloads.next()).generate(generator, null);
				}
			}
			generator.writeEnd();

			generator.writeEnd();

			Services.notifications.localsChanged();
		}
		LogWrapper.getLogger().info("Database backup complete.");
	}

	public static void restoreDatabase(JFrame origin, Path f)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					restoreDatabaseInternal(origin, f);
				}
				catch (IOException | SQLException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to restore database.", e);
				}
			}
		});
	}
	
	private synchronized static void restoreDatabaseInternal(JFrame origin, Path f) throws IOException, SQLException
	{
		try (ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();)
		{
			DbTables.deleteDb(wrapper);
		}
		DbRestoreProgress dbRestoreProgress = new DbRestoreProgress(Files.size(f));
		if (origin != null)
		{
			dbRestoreProgress.setLocation(origin.getLocation());
		}
		dbRestoreProgress.setVisible(true);
		
		try (CountingInputStream newInputStream = new CountingInputStream(Files.newInputStream(f));
				 JsonParser parser = TrackObjectUtils.createParser(newInputStream);
				 ConnectionWrapper wrapper = Services.h2DbCache.getThreadConnection();)
		{
			// Should break up the counting input stream class into two different classes...
			newInputStream.setRawMode(true);
			
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
						Services.notifications.remotesChanged();
						break;
					case "directories":
						dbRestoreProgress.setState("Reading directories");
						readDirectories(parser, wrapper, dbRestoreProgress, newInputStream);
						Services.notifications.localsChanged();
						break;
					case "files":
						dbRestoreProgress.setState("Reading files");
						readFiles(parser, wrapper, dbRestoreProgress, newInputStream);
						Services.notifications.localsChanged();
						break;
					case "permissions":
						dbRestoreProgress.setState("Reading permissions");
						readPermissions(parser, wrapper, dbRestoreProgress, newInputStream);
						Services.notifications.localsChanged();
						break;
					case "messages":
						dbRestoreProgress.setState("Reading messages");
						readMessages(parser, wrapper, dbRestoreProgress, newInputStream);
						Services.notifications.localsChanged();
						break;
					case "downloads":
						dbRestoreProgress.setState("Reading downloads");
						readDownloads(parser, wrapper, dbRestoreProgress, newInputStream);
						Services.notifications.localsChanged();
						break;
					}
				}
			}
		}
		finally
		{
			dbRestoreProgress.done();
			if (origin == null)
			{
				dbRestoreProgress.dispose();
			}
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

	private static void readDownloads(JsonParser parser,
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
				new DownloadBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}

	private static void readMessages(JsonParser parser,
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
				new MessageBackup(parser).save(wrapper);
				p.setProgress(newInputStream.getSoFar());
			}
		}
	}
}
