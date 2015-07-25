
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

import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AddMachine;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackerEntry;

public class TrackerFrame extends BrowserFrame
{
	LinkedList<ClientTrackerConnection> connections = new LinkedList<>();
	
	
	@Override
	protected void runLater(Runnable runnable)
	{
		Services.userThreads.execute(runnable);
	}

	@Override
	protected AddTracker createAddTracker()
	{
		AddTracker addTracker = new AddTracker(this) {
			@Override
			protected void addTracker(TrackerEntry entry)
			{
				Services.trackers.add(entry);
				Services.trackers.save(Services.settings.trackerFile.getPath());
			}
		};
		Services.notifications.registerWindow(addTracker);
		return addTracker;
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
			UserActions.show(machine);
		}
		else if (JOptionPane.showConfirmDialog(this, "This machine is not currently in the database, would you like to add it?", "Not currently in database", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			AddMachine addMachine = new AddMachine(currentMachine.getIp() + ":" + currentMachine.getPort());
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
		 Services.notifications.registerWindow(makeComment);
		makeComment.setAlwaysOnTop(true);
		makeComment.setVisible(true);
	}


	@Override
	protected void trackerAction1()
	{
		runLater(() -> {
		        if (currentClient == null) return;
		        currentClient.sync();
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
			listener.receiveTracker(client.getEntry());
		}
	}

	@Override
	protected TrackerClient createTrackerClient(TrackerEntry entry)
	{
		return Services.trackers.getClient(entry);
	}

	@Override
	protected boolean trackAction2Enabled()
	{
		return currentClient != null;
	}
	@Override
	protected boolean machineAction2Enabled()
	{
		return currentClient != null && currentMachine != null;
	}

	@Override
	protected void commentsChanged()
	{
		Services.colors.childrenChanged(this, getCommentPanel());
	}
}
