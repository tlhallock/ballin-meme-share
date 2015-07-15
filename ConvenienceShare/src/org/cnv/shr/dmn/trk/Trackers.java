
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;


public class Trackers
{
	private HashMap<String, ClientTrackerClient> trackers = new HashMap<>();
	
	/* We will accept trackers from these machines. */
	private HashSet<String> acceptingTrackersFrom = new HashSet<>();

	public void add(String url, int portBegin, int portEnd, boolean supportsMetadata)
	{
		ClientTrackerClient client = new ClientTrackerClient(new TrackerEntry(url, portBegin, portEnd, supportsMetadata));
		trackers.put(client.getAddress(), client);
	}

	public ClientTrackerClient add(TrackerEntry entry)
	{
		ClientTrackerClient client = new ClientTrackerClient(entry);
		trackers.put(client.getAddress(), client);
		return client;
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
	
    public Collection<ClientTrackerClient> getClients()
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

		public TrackerClient getClient(TrackerEntry entry)
		{
			ClientTrackerClient clientTrackerClient = trackers.get(entry.getAddress());
			if (clientTrackerClient == null)
			{
				return add(entry);
			}
			return clientTrackerClient;
		}

	public static void launchTracker(boolean interactive)
	{
		LogWrapper.getLogger().info("Launching tracker.");
		Path jarPath = ProcessInfo.getJarPath(Main.class);
		Path trackerJar = jarPath.resolve("Tracker.jar").toAbsolutePath();
		if (!Files.exists(trackerJar))
		{
			LogWrapper.getLogger().info("Unable to find tracker. Should be at " + trackerJar);
			if (interactive)
			{
				JOptionPane.showMessageDialog(Services.notifications.getCurrentContext(), "Could not find tracker jar at " + trackerJar, "Could not find tracker.", JOptionPane.WARNING_MESSAGE);
			}
			return;
		}

		LinkedList<String> arguments = new LinkedList<>();
		arguments.add(ProcessInfo.getJavaBinary());
		arguments.add("-jar");
		arguments.add(trackerJar.toString());

		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(jarPath.toFile());
		builder.command(arguments);
		try
		{
			builder.start();
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to launch tracker", ex);
		}
	}
	
	public void trackersRequested(Machine machine)
	{
		String identifier = machine.getIdentifier();
		synchronized (acceptingTrackersFrom)
		{
			acceptingTrackersFrom.add(identifier);
		}
		Misc.timer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				synchronized (acceptingTrackersFrom)
				{
					acceptingTrackersFrom.remove(identifier);
				}
			}}, 10 * 60 * 1000);
	}
	
	public boolean shouldAcceptTrackersFrom(Machine machine)
	{
		synchronized (acceptingTrackersFrom)
		{
			return acceptingTrackersFrom.contains(machine.getIdentifier());
		}
	}
	
	public AlternativeAddresses findAlternativeUrls(String machineIdentifier)
	{
		AlternativeAddresses addresses = new AlternativeAddresses();
		for (ClientTrackerClient client : getClients())
		{
			addresses.add(client.getUpdatedMachineInfo(machineIdentifier));
		}
		return addresses;
	}
}
