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

	@Override
	protected void foundTracker(TrackerEntry entry)
	{
		Services.trackers.add(entry);
	}
	
	public void requestSeeders(FileEntry remoteFile, Collection<Seeder> seeders)
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_SEEDERS))
		{
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

				Services.userThreads.execute(new Runnable()
				{
					@Override
					public void run()
					{
						addSeeder(remoteFile, entry);
					}
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
			Communication openConnection = Services.networkManager.openConnection(entry.getIp() + ":" + entry.getPortBegin() /* TODO: */, false);
			if (openConnection == null)
			{
				return;
			}
			openConnection.send(new LookingFor(remoteFile));
			openConnection.finish();
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
			Logger.getLogger(TrackerClient.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
