package org.cnv.shr.dmn.trk;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Seeder;
import org.cnv.shr.gui.tbl.DbJTable.CloseableIt;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.LookingFor;
import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.LogWrapper;

public class TrackerClient
{
	private TrackerEntry trackerEntry;
	
	public TrackerClient(TrackerEntry entry)
	{
		this.trackerEntry = new TrackerEntry(entry);
	}

  TrackerConnection connect(TrackerAction action) throws Exception
  {
  	return connect(new TrackerRequest(action));
  }
	/**
	 * Throw exception. Json throws runtime exceptions. (Maybe only json exceptions?)
	 */
	private TrackerConnection connect(TrackerRequest request) throws Exception
	{
		Exception lastException = null;
		TrackerConnection connection = null;
		for (int port = trackerEntry.getBeginPort(); port < trackerEntry.getEndPort(); port++)
		{
			try
			{
				connection = new TrackerConnection(trackerEntry.getIp(), port);
				break;
			}
			catch (IOException ex)
			{
				lastException = ex;
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect on port " + port, ex);
			}
		}
		
		if (connection == null)
		{
			throw new IOException("Not able to connect on any ports.", lastException);
		}
		else
		{
			connection.connect(request);
			return connection;
		}
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

	public TrackerEntry getEntry()
	{
		return trackerEntry;
	}

	public String getAddress()
	{
		return trackerEntry.getAddress();
	}

	CloseableIt<MachineEntry> list(int start) throws Exception
	{
		TrackerRequest trackerRequest = new TrackerRequest(TrackerAction.LIST_ALL_MACHINES);
		trackerRequest.setParameter("offset", String.valueOf(start));
		TrackerConnection connection = connect(trackerRequest);
		TrackObjectUtils.openArray(connection.parser);
		CloseableIt<MachineEntry> iterator = new CloseableIt<MachineEntry>()
		{
			private MachineEntry next;

			{
				MachineEntry entry = new MachineEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
			}

			@Override
			public boolean hasNext()
			{
				return  next != null && !connection.socket.isClosed();
			}

			@Override
			public MachineEntry next()
			{
				MachineEntry returnValue = next;
				if (returnValue == null)
				{
					return null;
				}
				MachineEntry entry = new MachineEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
				return returnValue;
			}

			@Override
			public void close()
			{
				next = null;
				if (connection.socket.isClosed())
				{
					return;
				}
				try
				{
					connection.generator.writeEnd();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
				try
				{
					connection.close();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
			}
		};
		return iterator;
	}

	CloseableIt<CommentEntry> listComments(MachineEntry machine) throws Exception
	{
		TrackerConnection connection = connect(TrackerAction.LIST_RATINGS);
		machine.print(connection.generator);
		connection.generator.flush();
		
		TrackObjectUtils.openArray(connection.parser);
		CloseableIt<CommentEntry> iterator = new CloseableIt<CommentEntry>()
		{
			private CommentEntry next;

			{
				CommentEntry entry = new CommentEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
			}

			@Override
			public boolean hasNext()
			{
				return  next != null && !connection.socket.isClosed();
			}

			@Override
			public CommentEntry next()
			{
				CommentEntry returnValue = next;
				if (returnValue == null)
				{
					return null;
				}
				CommentEntry entry = new CommentEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
				return returnValue;
			}

			@Override
			public void close()
			{
				next = null;
				if (connection.socket.isClosed())
				{
					return;
				}
				try
				{
					connection.generator.writeEnd();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
				try
				{
					connection.close();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
			}
		};
		return iterator;
	}

	void postComment(CommentEntry comment) throws Exception
	{
		try (TrackerConnection connection = connect(TrackerAction.POST_COMMENT))
		{
			comment.print(connection.generator);
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();
		}
	}

	void addOthers()
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
		}
		catch (Exception ex)
		{
			Logger.getLogger(TrackerClient.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void sync()
	{
		trackerEntry.setSync(true);
		Services.trackers.save(Services.settings.trackerFile.getPath());
		Services.trackers.kickSyncers(this);
	}
	
	
	
	
	
	
	

	
	public void requestSeeders(SharedFile remoteFile, Collection<Seeder> seeders)
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

	private void addSeeder(SharedFile remoteFile, MachineEntry entry)
	{
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
}
