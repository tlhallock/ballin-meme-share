
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



package org.cnv.shr.dmn;

import java.sql.SQLException;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.trk.AddTracker;
import org.cnv.shr.dmn.trk.BrowserFrame;
import org.cnv.shr.dmn.trk.TrackerClient;
import org.cnv.shr.dmn.trk.TrackerTrackerClient;
import org.cnv.shr.track.Receiver;
import org.cnv.shr.track.Track;
import org.cnv.shr.track.TrackerStore;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;

public class TrackerGui extends BrowserFrame
{
	private TrackerStore store;
	
	@Override
	protected void remoceClient(TrackerClient client)
	{
		if (client.equals(Track.LOCAL_TRACKER))
		{
			JOptionPane.showMessageDialog(this, 
					"Unable to delete local tracker!", 
					"Unable to delete local tracker.", 
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		store.removeTracker(client.getEntry());
	}

	@Override
	protected void machineAction2()
	{
		// delete the current machine...
		MachineEntry machine = this.currentMachine;
		if (machine == null)
		{
			return;
		}

		if (!currentClient.represents(Track.LOCAL_TRACKER))
		{
			LogWrapper.getLogger().info("Unable to delete from remote machine.");
			return;
		}
		
		store.removeMachine(machine);
		currentMachine = null;
		refreshAll();
	}

	@Override
	protected void machineAction1()
	{
		// add a new machine...
		if (currentClient == null || currentMachine == null)
		{
			return;
		}
		if (currentClient.represents(Track.LOCAL_TRACKER))
		{
			LogWrapper.getLogger().info("Remove Cached Info (Not yet supported.)");
			return;
		}
		
		store.machineFound(currentMachine, 0);
	}
	
	@Override
	protected void trackerAction1()
	{
		runLater(new Runnable() {
		    @Override
			public void run()
			{
				TrackerClient client = currentClient;
				if (client == null)
				{
					return;
				}
				
				if (client.represents(Track.LOCAL_TRACKER))
				{
					LogWrapper.getLogger().info("Add new machine info (Not supported yet).");
					return;
				}
				
				client.sync();
			}
		});
	}

	@Override
	protected String getMachineText1()
	{
		if (currentClient == null || currentMachine == null)
		{
			return "No machine selected";
		}
		if (currentClient.represents(Track.LOCAL_TRACKER))
		{
			return "Delete cached info";
		}
		return "Add machine to local Tracker";
	}

	@Override
	protected String getMachineText2()
	{
		return "Delete";
	}

	@Override
	protected String getTrackerText1()
	{
		if (currentClient == null)
		{
			return "No Tracker selected";
		}
		if (currentClient.represents(Track.LOCAL_TRACKER))
		{
			return "Add New Machine";
		}
		return "Add Tracker's Machines";
	}


	@Override
	protected void runLater(Runnable runnable)
	{
		Track.threads.execute(runnable);
	}

	@Override
	protected AddTracker createAddTracker()
	{
		return new AddTracker(this)
		{
			@Override
			protected void addTracker(TrackerEntry entry)
			{
				store.addTracker(entry);
			}
		};
	}

	@Override
	protected void listClients(TrackerListener listener)
	{
		getStore().listTrackers(new Receiver<TrackerEntry>()
		{
			@Override
			public void receive(TrackerEntry entry)
			{
				listener.receiveTracker(entry);
			}

			@Override
			public void done() {}
		});
	}
	
	private TrackerStore getStore()
	{
		if (store != null)
		{
			return store;
		}
		try
		{
			return store = new TrackerStore();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Unable to create tracker store", e);
			throw new RuntimeException("Unable to create tracker store");
		}
	}

	@Override
	protected TrackerClient createTrackerClient(TrackerEntry entry)
	{
		return new TrackerTrackerClient(entry, store);
	}

	@Override
	protected boolean trackAction2Enabled()
	{
		return currentClient != null && !currentClient.represents(Track.LOCAL_TRACKER);
	}

	@Override
	protected boolean machineAction2Enabled()
	{
		return currentClient != null && currentMachine != null && currentClient.represents(Track.LOCAL_TRACKER);
	}
}
