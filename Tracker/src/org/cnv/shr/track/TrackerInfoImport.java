package org.cnv.shr.track;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class TrackerInfoImport extends Thread
{
	private Path directory;
	private TrackerStore store;
	private WatchService service;
	
	private boolean quit = false;
	
	public TrackerInfoImport(Path root) throws IOException, SQLException
	{
		this.directory = root;
		service = root.getFileSystem().newWatchService();
		store = new TrackerStore();
	}
	
	public void quit()
	{
		quit = true;
	}
	
	public void run()
	{
		Misc.ensureDirectory(directory, false);
		WatchKey register;
		try
		{
			register = directory.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to watch directory " + directory, e1);
			return;
		}
		while (!quit)
		{
			WatchKey watchKey;
			try
			{
				watchKey = service.poll(60, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted", ex);
				break;
			}
			if (watchKey == null)
			{
				continue;
			}
			for (WatchEvent<?> event : watchKey.pollEvents())
			{
				Object context2 = event.context();
				Path context;
				try
				{
					context = (Path) context2;
				}
				catch (ClassCastException e1)
				{
					LogWrapper.getLogger().log(Level.WARNING, "Found something not a path: " + context2.getClass().getName(), e1);
					continue;
				}
				try
				{
					importTrackerInfo(context);
				}
				catch (IOException | SQLException e)
				{
					LogWrapper.getLogger().log(Level.WARNING, "Unable to add file at path " + context, e);
				}
			}
			watchKey.reset();
		}

		register.cancel();
	}
	
	public synchronized void importTrackerInfo(Path f) throws IOException, SQLException
	{
    LogWrapper.getLogger().info("Import trackers/machines from file " + f);
		try (CountingInputStream newInputStream = new CountingInputStream(Files.newInputStream(f));
				 JsonParser parser = TrackObjectUtils.createParser(newInputStream, false);)
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
						LogWrapper.getLogger().info("Reading machines");
						readMachineEntries(parser, newInputStream);
						break;
					case "trackers":
						LogWrapper.getLogger().info("Reading trackers");
						readTrackerEntries(parser, newInputStream);
						break;
					}
				}
			}
		}
	}

	private void readMachineEntries(JsonParser parser, CountingInputStream newInputStream)
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
				store.machineFound(new MachineEntry(parser), 0);
			}
		}
	}

	private void readTrackerEntries(JsonParser parser, CountingInputStream newInputStream)
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
				store.addTracker(new TrackerEntry(parser));
			}
		}
	}

	public void addExisting() throws IOException
	{
		Misc.ensureDirectory(directory, false);
		addExisting(directory);
	}
	private void addExisting(Path current) throws IOException
	{
		try (DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(current);)
		{
			for (Path child : newDirectoryStream)
			{
				if (Files.isDirectory(child))
				{
					addExisting(child);
				}
				if (Files.isRegularFile(child))
				{
					try
					{
						importTrackerInfo(child);
					}
					catch (SQLException e)
					{
						LogWrapper.getLogger().log(Level.WARNING, "Unable to add file at path " + child, e);
					}
				}
			}
		}
	}
}
