
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



package org.cnv.shr.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionParams.AutoCloseConnectionParams;
import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.ClientTrackerClient;
import org.cnv.shr.gui.AddMachine.AddMachineParams;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.MirrorDirectory;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.FindTrackers;
import org.cnv.shr.msg.GetPermission;
import org.cnv.shr.msg.ListRoots;
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;

public class UserActions
{
	public static void removeMachine(final Machine remote)
	{
		Services.userThreads.execute(() ->
		{
				DbMachines.delete(remote);
				// Is the first of these two really necessary?
				Services.notifications.remoteChanged(remote);
				Services.notifications.remotesChanged();
		});
	}

	public static void addMachine(final String url, final AddMachineParams params)
	{
		Services.networkManager.openConnection(new AutoCloseConnectionParams(url, params.acceptKeys, "Add machine") {
			public void onFail()
			{
				Services.keyManager.addPendingAuthentication(Services.settings.keysFile.getPath(), url);
			}
			@Override
			public void opened(Communication connection) throws Exception
			{
				Machine machine = connection.getMachine();
				if (params.message)
				{
					// enable messaging
					machine.setAllowsMessages(true);
				}

				if (params.visible)
				{
					if (params.share)
					{
						machine.setWeShare(SharingState.DOWNLOADABLE);
					}
					else
					{
						machine.setWeShare(SharingState.SHARE_PATHS);
					}
				}

				if (params.pin)
				{
					machine.setPinned(true);
					machine.tryToSave();
				}

				machine.tryToSave();

				if (params.open)
				{
					show(machine);
				}
			}
		});
	}

	public static void syncRoots(JFrame origin, final Machine m)
	{
		Services.userThreads.execute(() -> {
				syncRootsNow(origin, m);
		});
	}
	
	private static void syncRootsNow(JFrame origin, final Machine m)
	{
    LogWrapper.getLogger().info("Synchronizing roots with " + m.getName());
		Services.networkManager.openConnection(new AutoCloseConnectionParams(origin, m, false, "Synchronize roots") {
			@Override
			public void opened(Communication connection) throws Exception
			{
				connection.send(new ListRoots());
			}
		});
	}

	public static void findMachines(JFrame origin, final Machine m)
	{
		findMachines(origin, m, new HashSet<String>());
	}


	public static void findTrackers(JFrame origin, Machine machine)
	{
		Services.userThreads.execute(() -> {
				LogWrapper.getLogger().info("Requesting trackers from " + machine.getName());
				Services.trackers.trackersRequested(machine);

				Services.networkManager.openConnection(new AutoCloseConnectionParams(origin, machine, false, "Find trackers") {
					@Override
					public void opened(Communication connection) throws Exception
					{
						connection.send(new FindTrackers());
					}
				});
		});
	}
	
	public static void findMachines(JFrame origin, final Machine m, HashSet<String> foundUrls)
	{
		Services.userThreads.execute(() -> {
			LogWrapper.getLogger().info("Requesting peers from " + m.getName());

			String url = m.getIp() + ":" + m.getPort();
			synchronized (foundUrls)
			{
				if (!foundUrls.add(url))
				{
					return;
				}
			}

			Services.networkManager.openConnection(new AutoCloseConnectionParams(origin, m, false, "Find more machines") {
				@Override
				public void opened(Communication connection) throws Exception
				{
					connection.send(new FindMachines());
				}
			});
		});
	}

	public static void syncAllLocals(JFrame origin)
	{
		Services.userThreads.execute(() ->
			{
				LinkedList<LocalDirectory> locals = new LinkedList<>();
				try (final DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();)
				{
					while (listLocals.hasNext())
					{
						locals.add(listLocals.next());
					}
					for (LocalDirectory local : locals)
					{
						userSync(origin, local, null);
					}
					// Services.db.removeUnusedPaths();
					Services.notifications.localsChanged();
				}
		});
	}

	public static void syncRemote(JFrame origin, final RootDirectory directory)
	{
		Services.userThreads.execute(() ->
		{
			directory.synchronize(origin, null);
		});
	}

	public static LocalDirectoryView showLocal(LocalDirectory root, boolean exitOnSave)
	{
		LocalDirectoryView localDirectoryView = new LocalDirectoryView(root, exitOnSave);
		Services.notifications.registerWindow(localDirectoryView);
		localDirectoryView.setVisible(true);
		LogWrapper.getLogger().info("Displaying " + root.getName());
		return localDirectoryView;
	}

	public static LocalDirectory addLocalImmediately(Path localDirectory, String name, boolean mirror)
	{
		try
		{
			LogWrapper.getLogger().info("Sharing " + localDirectory);
			
			localDirectory = localDirectory.toAbsolutePath();
			if (name == null)
			{
				name = localDirectory.getFileName().toString();
			}
			
			LocalDirectory local2 = DbRoots.getLocal(localDirectory);
			if (local2 != null)
			{
				LogWrapper.getLogger().info("There is already a local directory at " + localDirectory);
				return local2;
			}

			int currentAttempt = 1;
			String alternative = name;
			local2 = DbRoots.getLocalByName(name);
			while (local2 != null)
			{
				local2 = DbRoots.getLocalByName(name = (alternative + ++currentAttempt));
				LogWrapper.getLogger().info("Local directory with this name already exists.\nTrying " + name + ".");
			}

			LocalDirectory local = mirror ? new MirrorDirectory(localDirectory, name) : new LocalDirectory(localDirectory, name);
			local.tryToSave();
			if (local.getId() == null)
			{
				local = DbRoots.getLocalByName(local.getName());
			}
			if (local != null)
			{
				Services.notifications.localChanged(local);
			}
			return local;
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get file path to share: " + localDirectory, e1);
			return null;
		}
	}

	public static void userSync(JFrame origin, final LocalDirectory d, final List<? extends SynchronizationListener> listeners)
	{
		Services.userThreads.execute(() -> { d.synchronize(origin, listeners); });
	}

	public static void remove(final RootDirectory l)
	{
		l.stopSynchronizing();
		DbRoots.deleteRoot(l, true);
	}

	public static void shareWith(final Machine m, final SharingState share)
	{
		m.setWeShare(share);
		m.tryToSave();
	}

	public static void download(final SharedFile remote)
	{
		Services.downloads.downloadThreads.execute(() -> {
			try
			{
				Services.downloads.download(remote);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to download " + remote, e);
			}
		});
	}

	public static void debug()
	{
		Services.userThreads.execute(() -> { DbTables.debugDb(); } );
	}

	public static void deleteDb()
	{
		Services.userThreads.execute(() -> {
			try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
			{
				DbTables.deleteDb(c);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		});
	}

	public static void closeConnection()
	{
	}

	public static void changeKeys()
	{
		
	}
	
	public static void showGui()
	{
		showGui(null);
	}

	public static void showGui(SplashScreen screen)
	{
		Services.userThreads.execute(() ->
		{
			try
			{
				Application application = new Application();
				Services.notifications.registerWindow(application);
				application.setVisible(true);
				application.refreshAll();
				
				if (screen != null)
				{
					screen.dispose();
				}
			}
			catch (Exception ex)
			{
				LogWrapper.getLogger().log(Level.SEVERE, "Unable to start GUI.\nQuiting.", ex);
				Services.quiter.quit();
			}
		});
	}

	static void findMachines(JFrame origin)
	{
		Services.userThreads.execute(() ->
		{
			HashSet<String> foundUrls = new HashSet<>();
			foundUrls.add(Services.localMachine.getIp() + ":" + Services.localMachine.getPort());
			
			LinkedList<ClientTrackerClient> list = new LinkedList<>();
			synchronized (Services.trackers)
			{
				list.addAll(Services.trackers.getClients());
			}
			for (ClientTrackerClient client : list)
			{
				findMachines(origin, client, foundUrls);
			}
			
			try (DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();)
			{
				while (listRemoteMachines.hasNext())
				{
					findMachines(origin, listRemoteMachines.next(), foundUrls);
				}
			}
		});
	}

	static void findMachines(JFrame origin, ClientTrackerClient client, HashSet<String> foundUrls)
	{
		int currentPage = 0;
		boolean hasMore = true;
		while (hasMore)
		{
			hasMore = false;
			try (CloseableIterator<MachineEntry> machineEntries = client.list(currentPage);)
			{
				while (machineEntries.hasNext())
				{
					MachineEntry next = machineEntries.next();
					if (next == null)
					{
						continue;
					}
					hasMore = true;
					
					Machine machine = DbMachines.getMachine(next.getIdentifer());
					if (machine != null)
					{
						continue;
					}
					String url = next.getIp() + ":" + next.getPort();
					if (!foundUrls.add(url))
					{
						continue;
					}
					
					if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
							origin,
							"Found a machine at address " + url + ".\n"
									+ "The machine's name is " + next.getName() + ".\n"
									+ "Would you like to add it?", 
									"Found a machine",
									JOptionPane.YES_NO_OPTION))
					{
						AddMachine addMachine = new AddMachine(url);
						Services.notifications.registerWindow(addMachine);
						addMachine.setVisible(true);
						addMachine.setAlwaysOnTop(true);
					}
				}
				
				currentPage += TrackerEntry.MACHINE_PAGE_SIZE;
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to get page " + currentPage + " from " + client.getEntry(), e);
			}
		}
	}

    static void findTrackers()
    {
  		Services.userThreads.execute(() ->
  		{
  			LinkedList<ClientTrackerClient> list = new LinkedList<>();
  			synchronized (Services.trackers)
  			{
  				list.addAll(Services.trackers.getClients());
  			}
  			for (ClientTrackerClient client : list)
  			{
  				client.addOthers();
  			}
  		});
  	}
    
	public static void syncPermissions(JFrame origin, Machine machine)
	{
		syncRootsNow(origin, machine);

		// Is the rest of this really necessary?
		// Permissions should be synced inside of syncRoots...
		Services.networkManager.openConnection(new AutoCloseConnectionParams(origin, machine, false, "Check permissions") {
			@Override
			public void opened(Communication connection) throws Exception
			{
				try (DbIterator<RootDirectory> list = DbRoots.list(machine);)
				{
					connection.send(new GetPermission());
					while (list.hasNext())
					{
						connection.send(new GetPermission((RemoteDirectory) list.next()));
					}
				}
			}
		});
	}

	public static void show(Machine machine)
	{
		if (machine == null)
		{
			LogWrapper.getLogger().info("Cannot show null machine");
			return;
		}

		final MachineViewer viewer = new MachineViewer(machine);
		Services.notifications.registerWindow(viewer);
		viewer.setTitle("Machine " + machine.getName());
		viewer.setVisible(true);
		LogWrapper.getLogger().info("Showing remote " + machine.getName());

		if (machine.isLocal())
		{
			return;
		}
		
		Services.userThreads.execute(() -> { syncPermissions(viewer, machine); });
	}

	public static boolean checkIfMachineShouldNotReplaceOld(String ident, String ip, int port)
	{
		Machine findAnExistingMachine = DbMachines.findAnExistingMachine(ip, port);
		return !(findAnExistingMachine == null
				|| findAnExistingMachine.getIdentifier().equals(ident)
				|| UserActions.userAcceptsNewIdentifier(ident, findAnExistingMachine, ip + ":" + port));
	}
	private static boolean userAcceptsNewIdentifier(String newidentifer, Machine machine, String url)
	{
		if (machine != null && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
				Services.notifications.getCurrentContext(),
				"For machine at " + url + " we expected to find an identifier of\n"
					+ "\"" + machine.getIdentifier() + "\"\nbut instead it was\n\"" + newidentifer + "\n"
					+ "Would you like to remove the previous machine and add the new one?",
				"Found wrong machine at " + newidentifer,
				JOptionPane.YES_NO_OPTION))
		{
			return false;
		}
		DbMachines.delete(machine);
		return true;
	}
}
