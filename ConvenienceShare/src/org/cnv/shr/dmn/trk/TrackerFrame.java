
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

import javax.swing.JOptionPane;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AddMachine;
import org.cnv.shr.gui.MachineViewer;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;

public class TrackerFrame extends BrowserFrame
{
	{
		Services.notifications.registerWindow(this);
	}
	@Override
	protected void runLater(Runnable runnable)
	{
		Services.userThreads.execute(runnable);
	}

	@Override
	protected AddTracker createAddTracker()
	{
		return new AddTracker(this) {
			@Override
			protected void addTracker(TrackerEntry entry)
			{
				Services.trackers.add(entry);
				Services.trackers.save(Services.settings.trackerFile.getPath());
			}
		};
	}
	
	@Override
	protected void remoceClient(TrackerClient client)
	{
		Services.trackers.remove(client);
		Services.trackers.save(Services.settings.trackerFile.getPath());
	}

	@Override
	protected void machineAction2()
	{
		if (currentMachine == null)
			return;

		Machine machine = DbMachines.getMachine(currentMachine.getIdentifer());
		if (machine != null)
		{
			final MachineViewer viewer = new MachineViewer(machine);
			Services.notifications.registerWindow(viewer);
			viewer.setTitle("Machine " + machine.getName());
			viewer.setVisible(true);
			LogWrapper.getLogger().info("Showing remote " + machine.getName());

		}
		else if (JOptionPane.showConfirmDialog(this, "This machine is not currently in the database, would you like to add it?", "Not currently in database", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			AddMachine addMachine = new AddMachine(currentMachine.getIp() + ":" + currentMachine.getPortBegin());
			addMachine.setAlwaysOnTop(true);
			Services.notifications.registerWindow(addMachine);
			addMachine.setVisible(true);
		}
	}

	@Override
	protected void machineAction1()
	{
		if (currentClient == null || currentMachine == null)
			return;
		MakeComment makeComment = new MakeComment(currentClient, currentMachine.getIdentifer(), this);
		// Services.notification.registerWindow(makeComment);
		makeComment.setAlwaysOnTop(true);
		makeComment.setVisible(true);
	}


	@Override
	protected void trackerAction1()
	{
		runLater(new Runnable() {
		    @Override
		    public void run() {
		        if (currentClient == null) return;
		        currentClient.sync();
		    }
		});
	}

  @Override protected String getMachineText1() { return "Add comment";          }
  @Override protected String getMachineText2() { return "Open";                 }
  @Override protected String getTrackerText1() { return "Upload file metadata"; }

	@Override
	protected void listClients(TrackerListener listener)
	{
		for (TrackerClient client : Services.trackers.getClients())
		{
			listener.receiveTracker(client);
		}
	}
}
