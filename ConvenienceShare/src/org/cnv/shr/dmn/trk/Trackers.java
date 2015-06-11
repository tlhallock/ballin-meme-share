
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
	private HashMap<String, ClientTrackerClient> trackers = new HashMap<>();

	public void add(String url, int portBegin, int portEnd)
	{
		ClientTrackerClient client = new ClientTrackerClient(new TrackerEntry(url, portBegin, portEnd));
		trackers.put(client.getAddress(), client);
	}

	public void add(TrackerEntry entry)
	{
		ClientTrackerClient client = new ClientTrackerClient(entry);
		trackers.put(client.getAddress(), client);
	}
	
	public void save(Path trackersFile)
	{
		Misc.ensureDirectory(trackersFile, true);
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(trackersFile));)
		{
			generator.writeStartArray();
			for (TrackerClient client : trackers.values())
			{
				client.getEntry().generate(generator);
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
				ClientTrackerClient loadedClient = new ClientTrackerClient(entry);
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

    public Iterable<ClientTrackerClient> getClients()
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
