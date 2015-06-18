package org.cnv.shr.track;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.LogWrapper;

public class TrackerInfoImport
{
	public synchronized static void importTrackerInfo(Path f, TrackerStore store) throws IOException, SQLException
	{
		try (CountingInputStream newInputStream = new CountingInputStream(Files.newInputStream(f));
				 JsonParser parser = TrackObjectUtils.createParser(newInputStream);)
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
						LogWrapper.getLogger().info("Reading machines");
						readMachineEntries(parser, newInputStream, store);
						break;
					case "trackers":
						LogWrapper.getLogger().info("Reading trackers");
						readTrackerEntries(parser, newInputStream, store);
						break;
					}
				}
			}
		}
	}

	private static void readMachineEntries(JsonParser parser, CountingInputStream newInputStream, TrackerStore store)
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

	private static void readTrackerEntries(JsonParser parser, CountingInputStream newInputStream, TrackerStore store)
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
}
