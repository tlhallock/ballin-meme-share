
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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.json.JsonException;

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
          LogWrapper.getLogger().info("Synchronizing roots with " + m.getName());
					Communication openConnection = Services.networkManager.openConnection(m, false);
					if (openConnection == null)
					{
						return;
					}
					openConnection.send(new ListRoots());
					openConnection.finish();
					Services.notifications.remoteChanged(openConnection.getMachine());
				}
				catch (IOException | JsonException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to sync roots with " + url, e);
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
					LogWrapper.getLogger().info("Requesting peers from " + m.getName());
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
				try (final DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();)
				{
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
