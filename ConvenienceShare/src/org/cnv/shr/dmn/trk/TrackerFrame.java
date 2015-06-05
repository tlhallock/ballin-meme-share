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
