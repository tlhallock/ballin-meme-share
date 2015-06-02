package org.cnv.shr.dmn.trk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;


public class Trackers
{
	private LinkedList<TrackerClient> trackers = new LinkedList<>();
	
	void add(String url, int portBegin, int portEnd)
	{
		trackers.add(new TrackerClient(new TrackerEntry(url, portBegin, portEnd)));
	}
	
	void save(Path trackersFile)
	{
		try (JsonGenerator generator = TrackObjectUtils.generatorFactory.createGenerator(Files.newOutputStream(trackersFile));)
		{
			generator.writeStartArray();
			for (TrackerClient client : trackers)
			{
				client.getEntry().print(generator);
			}
			generator.writeEnd();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save trackers", e);
		}
	}
	
	void load(Path trackersFile)
	{
		TrackerEntry entry = new TrackerEntry();
		try (JsonParser openArray = TrackObjectUtils.openArray(Files.newInputStream(trackersFile));)
		{
			while (TrackObjectUtils.next(openArray, entry))
			{
				trackers.add(new TrackerClient(entry));
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save trackers", e);
		}
	}
	
	public static void main(String[] args)
	{
		Trackers trackers = new Trackers();
		trackers.load(Paths.get("foobar.json"));
		trackers.add("127.0.0.1", 15, 56);
		trackers.save(Paths.get("foobar.json"));
	}

    Iterable<TrackerClient> getClients()
    {
        return trackers;
    }
}
