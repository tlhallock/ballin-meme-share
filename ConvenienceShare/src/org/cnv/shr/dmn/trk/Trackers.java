package org.cnv.shr.dmn.trk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;


public class Trackers
{
	private HashMap<String, TrackerClient> trackers = new HashMap<>();

	public void add(String url, int portBegin, int portEnd)
	{
		TrackerClient client = new TrackerClient(new TrackerEntry(url, portBegin, portEnd));
		trackers.put(client.getAddress(), client);
	}

	public void add(TrackerEntry entry)
	{
		TrackerClient client = new TrackerClient(entry);
		trackers.put(client.getAddress(), client);
	}
	
	public void save(Path trackersFile)
	{
		Misc.ensureDirectory(trackersFile, true);
		try (JsonGenerator generator = TrackObjectUtils.generatorFactory.createGenerator(Files.newOutputStream(trackersFile));)
		{
			generator.writeStartArray();
			for (TrackerClient client : trackers.values())
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
	
	public void load(Path trackersFile)
	{
		TrackerEntry entry = new TrackerEntry();
		try (JsonParser openArray = TrackObjectUtils.openArray(Files.newInputStream(trackersFile));)
		{
			while (TrackObjectUtils.next(openArray, entry))
			{
				TrackerClient loadedClient = new TrackerClient(entry);
				trackers.put(loadedClient.getAddress(), loadedClient);
			}
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save trackers", e);
			save(trackersFile);
		}
	}
	
	public static void main(String[] args)
	{
		Trackers trackers = new Trackers();
		trackers.load(Paths.get("foobar.json"));
		trackers.add("127.0.0.1", 15, 56);
		trackers.save(Paths.get("foobar.json"));
	}

    public Iterable<TrackerClient> getClients()
    {
        return trackers.values();
    }

    void remove(TrackerClient client)
    {
        trackers.remove(client.getAddress());
    }

		public void kickSyncers(TrackerClient client)
		{
			Services.userThreads.execute(new TrackerSyncRunnable(client));
		}
}
