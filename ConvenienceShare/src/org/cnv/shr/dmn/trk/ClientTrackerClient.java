
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
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Seeder;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.LookingFor;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.LogWrapper;

public class ClientTrackerClient extends TrackerClient
{
	public ClientTrackerClient(TrackerEntry entry)
	{
		super(entry);
	}

	public void keyChanged()
	{
		try (TrackerConnection connection = connect(TrackerAction.POST_MACHINE))
		{
			connection.generator.writeEnd();
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
	}
	
	@Override
	public void sync()
	{
		trackerEntry.setSync(true);
		Services.trackers.save(Services.settings.trackerFile.getPath());
		Services.trackers.kickSyncers(this);
	}

	@Override
	protected ClientTrackerConnection createConnection(int port) throws IOException
	{
		return new ClientTrackerConnection(trackerEntry.getIp(), port);
	}

//	@Override
//	protected void foundTracker(TrackerEntry entry)
//	{
//		Services.trackers.add(entry);
//	}
	
	public void requestSeeders(FileEntry remoteFile, Collection<Seeder> seeders)
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_SEEDERS))
		{
			remoteFile.generate(connection.generator);
			connection.generator.flush();
			
			MachineEntry entry = new MachineEntry();
			TrackObjectUtils.openArray(connection.parser);

			while (TrackObjectUtils.next(connection.parser, entry))
			{
				// TODO: should add or connect when this doesn't exist...
				final Machine remote = DbMachines.getMachine(entry.getIdentifer());

				if (remote != null && alreadyHasSeeder(seeders, remote))
				{
					continue;
				}

				Services.downloads.downloadThreads.execute(() ->
				{
					addSeeder(remoteFile, entry);
				});
			}
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list seeders", ex);
		}
	}

	private void addSeeder(FileEntry remoteFile, MachineEntry entry)
	{
		LogWrapper.getLogger().info("Found seeder " + entry);
		
		try
		{
			Communication openConnection = Services.networkManager.openConnection(entry.getIp() + ":" + entry.getPortBegin() /* TODO: */, false, "Download file");
			if (openConnection == null)
			{
				return;
			}
			try
			{
			openConnection.send(new LookingFor(remoteFile));
			}
			finally
			{
				openConnection.finish();
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to request seeder.", e);
		}
	}

	private boolean alreadyHasSeeder(Collection<Seeder> seeders, final Machine remote)
	{
		synchronized (seeders)
		{
			for (Seeder seeder : seeders)
			{
				if (seeder.is(remote))
				{
					return true;
				}
			}
			return false;
		}
	}

	@Override
	protected void runLater(Runnable runnable)
	{
		Services.userThreads.execute(runnable);
	}
	
	public void addOthers()
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_TRACKERS))
		{
			TrackerEntry entry = new TrackerEntry();
			TrackObjectUtils.openArray(connection.parser);
			while (TrackObjectUtils.next(connection.parser, entry))
			{
				Services.trackers.add(entry);
			}
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();

			LogWrapper.getLogger().info("Added " + entry);
		}
		catch (Exception ex)
		{
			Logger.getLogger(TrackerClient.class.getName()).log(Level.INFO, "Unable to list trackers.", ex);
		}
	}
	
	

	public MachineEntry getUpdatedMachineInfo(String machineIdentifier)
	{
		TrackerRequest trackerRequest = new TrackerRequest(TrackerAction.GET_MACHINE);
		trackerRequest.setParameter("other", machineIdentifier);
		try (TrackerConnection connection = connect(trackerRequest))
		{
			MachineEntry entry = new MachineEntry();
			TrackObjectUtils.openArray(connection.parser);
			if (TrackObjectUtils.next(connection.parser, entry))
			{
				LogWrapper.getLogger().info("Found " + entry);
				return entry;
			}
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();
		}
		catch (Exception ex)
		{
			Logger.getLogger(TrackerClient.class.getName()).log(Level.INFO, "Unable to list trackers.", ex);
		}
		return null;
	}
}
