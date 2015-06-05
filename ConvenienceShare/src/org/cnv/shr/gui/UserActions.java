package org.cnv.shr.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AddMachine.AddMachineParams;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.ListRoots;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.util.LogWrapper;

public class UserActions
{
	public static void removeMachine(final Machine remote)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				DbMachines.delete(remote);
				// Is the first of these two really necessary?
				Services.notifications.remoteChanged(remote);
				Services.notifications.remotesChanged();
			}
		});
	}

	public static void addMachine(final String url, final AddMachineParams params)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Communication openConnection = Services.networkManager.openConnection(url, params.acceptKeys);
					if (openConnection == null)
					{
						Services.keyManager.addPendingAuthentication(
								Services.settings.keysFile.getPath(),
								url);
						return;
					}

					Machine machine = openConnection.getMachine();
					if (params.message)
					{
						// enable messaging
						machine.setAllowsMessages(true);
						machine.tryToSave();
					}

					if (params.visible)
					{
						if (params.share)
						{
							machine.setWeShare(SharingState.DOWNLOADABLE);
							machine.tryToSave();
						}
						else
						{
							machine.setWeShare(SharingState.SHARE_PATHS);
							machine.tryToSave();
						}
					}
					
					openConnection.send(new MachineFound());
					openConnection.send(new FindMachines());

					openConnection.finish();
					
					if (params.open)
					{
						final MachineViewer viewer = new MachineViewer(machine);
						Services.notifications.registerWindow(viewer);
						viewer.setTitle("Machine " + machine.getName());
						viewer.setVisible(true);
						LogWrapper.getLogger().info("Showing remote " + machine.getName());
					}
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to discover " + url, e);
				}
			}
		});
	}

	public static void syncRoots(final Machine m)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				String url = m.getUrl();
				try
				{
					Communication openConnection = Services.networkManager.openConnection(m, false);
					if (openConnection == null)
					{
						return;
					}
					openConnection.send(new ListRoots());
					openConnection.finish();
					Services.notifications.remoteChanged(openConnection.getMachine());
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to discover " + url, e);
				}
			}
		});
	}

	public static void findMachines(final Machine m)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Communication openConnection = Services.networkManager.openConnection(m, false);
					if (openConnection == null)
					{
						return;
					}
					openConnection.send(new FindMachines());
					openConnection.finish();
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to discover refresh " + m.getUrl(), e);
				}
			}
		});
	}

	public static void syncAllLocals()
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				LinkedList<LocalDirectory> locals = new LinkedList<>();
				final DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
				while (listLocals.hasNext())
				{
					locals.add(listLocals.next());
				}
				for (LocalDirectory local : locals)
				{
					userSync(local, null);
				}
				// Services.db.removeUnusedPaths();
				Services.notifications.localsChanged();
			}
		});
	}

	public static void syncRemote(final RootDirectory directory)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				directory.synchronize(null);
			}
		});
	}

	public static void queueLocal(final Path localDirectory, final String name)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				addLocalImmediately(localDirectory, name);
			}
		});
	}

	public static LocalDirectory addLocalImmediately(final Path localDirectory, final String name)
	{
		try
		{
			LogWrapper.getLogger().info("Sharing " + localDirectory);
			PathElement pathElement = DbPaths.getPathElement(localDirectory);

			LocalDirectory local = new LocalDirectory(pathElement, name);
			local.tryToSave();
			if (local.getId() == null)
			{
				local = DbRoots.getLocalByName(local.getName());
			}
			if (local != null)
			{
				DbPaths.pathLiesIn(pathElement, local);
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

	public static void userSync(final LocalDirectory d, final List<? extends SynchronizationListener> listeners)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				d.synchronize(listeners);
			}
		});
	}

	public static void remove(final RootDirectory l)
	{
		l.stopSynchronizing();
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				DbRoots.deleteRoot(l);
			}
		});
	}

	public static void shareWith(final Machine m, final SharingState share)
	{
		m.setWeShare(share);
		m.tryToSave();
	}

	public static void shareWith(Machine m, LocalDirectory local, boolean share)
	{
	}

	public static void download(final SharedFile remote)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Services.downloads.download(remote);
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to download " + remote, e);
				}
			}
		});
	}

	public static void debug()
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				DbTables.debugDb();
			}
		});
	}

	public static void deleteDb()
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
				{
					DbTables.deleteDb(c);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
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
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Application application = new Application();
					Services.notifications.registerWindow(application);
					application.setVisible(true);
					application.refreshAll();
				}
				catch (Exception ex)
				{
					LogWrapper.getLogger().log(Level.SEVERE, "Unable to start GUI.\nQuiting.", ex);
					Services.quiter.quit();
				}
			}
		});
	}
}
