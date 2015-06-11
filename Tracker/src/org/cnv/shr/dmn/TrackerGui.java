package org.cnv.shr.dmn;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.dmn.trk.AddTracker;
import org.cnv.shr.dmn.trk.BrowserFrame;
import org.cnv.shr.dmn.trk.TrackerClient;
import org.cnv.shr.dmn.trk.TrackerConnection;
import org.cnv.shr.track.Track;
import org.cnv.shr.track.TrackerStore;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class TrackerGui extends BrowserFrame
{
	private TrackerStore store;
	
	@Override
	protected void remoceClient(TrackerClient client)
	{
		store.removeTracker(client.getEntry());
	}

	@Override
	protected void machineAction2()
	{
		// delete the current machine...
	}

	@Override
	protected void machineAction1()
	{
		// add a new machine...
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

	@Override
	protected String getMachineText1()
	{
		return "Add";
	}

	@Override
	protected String getMachineText2()
	{
		return "Delete";
	}

	@Override
	protected String getTrackerText1()
	{
		return "a1";
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
		getStore().listTrackers(new TrackerStore.TrackerListener()
		{
			@Override
			public void receiveTracker(TrackerEntry entry)
			{
				listener.receiveTracker(new TrackerClient(entry) {
					@Override
					protected TrackerConnection createConnection(int port) throws IOException
					{
						return new TrackerConnection(entry.getAddress(), port) {
							@Override
							protected void sendDecryptedNaunce(byte[] naunceRequest, RSAPublicKey publicKey) {}

							@Override
							protected MachineEntry getLocalMachine()
							{
								return new MachineEntry(
										// Nothing really matters because we will fail authentication.
										"Not used", "Not used.", "Not used.", TrackerEntry.TRACKER_PORT_BEGIN, TrackerEntry.TRACKER_PORT_END, "Not used.");
							}};
					}

					@Override
					public void sync() {}

					@Override
					protected void foundTracker(org.cnv.shr.trck.TrackerEntry entry)
					{
						getStore().addTracker(entry);
					}

					@Override
					protected void runLater(Runnable runnable)
					{
						Track.threads.execute(runnable);
					}

					@Override
					public void addOthers()
					{
						LogWrapper.getLogger().info("Implement me!");
					}});
			}
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
}
