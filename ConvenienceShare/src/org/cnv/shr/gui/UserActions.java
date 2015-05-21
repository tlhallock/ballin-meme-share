package org.cnv.shr.gui;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
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
						Services.keyManager.addPendingAuthentication(url);
						return;
					}

					Machine machine = openConnection.getMachine();
					if (params.message)
					{
						// enable messaging
						machine.setAllowsMessages(true);
						machine.save();
					}

					if (params.visible)
					{
						if (params.share)
						{
							machine.setSharing(SharingState.DOWNLOADABLE);
							machine.save();
						}
						else
						{
							machine.setSharing(SharingState.SHARE_PATHS);
							machine.save();
						}
					}
					
					openConnection.send(new MachineFound());
					openConnection.send(new FindMachines());

					openConnection.finish();
				}
				catch (IOException e)
				{
					Services.logger.println("Unable to discover " + url);
					Services.logger.print(e);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
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
					Services.logger.println("Unable to discover " + url);
					Services.logger.print(e);
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
					Services.logger.println("Unable to discover refresh " + m.getUrl());
					Services.logger.print(e);
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
					sync(local);
				}
//				Services.db.removeUnusedPaths();
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

	public static void addLocal(final File localDirectory, final boolean sync, final String name)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Services.logger.println("Sharing " + localDirectory);
					PathElement pathElement = DbPaths.getPathElement(localDirectory.getCanonicalPath());
					
					LocalDirectory local = new LocalDirectory(pathElement, name);
					local.save();
					DbPaths.pathLiesIn(pathElement, local);
					Services.notifications.localChanged(local);
					
					if (sync)
					{
						sync(local);
					}
				}
				catch (SQLException | IOException e1)
				{
					Services.logger.println("Unable to get file path to share: " + localDirectory);
					Services.logger.print(e1);
				}
			}
		});
	}

	public static void sync(LocalDirectory d)
	{
		Application a = Services.application;
		d.synchronize(a == null ? null : Collections.singletonList(a.createLocalListener(d)));
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

	public static void shareWith(Machine m, boolean share)
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
			}
		});
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
					Services.logger.print(e);
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
				DbTables.deleteDb(Services.h2DbCache.getConnection());
			}
		});
	}

	public static void closeConnection()
	{
	}

	public static void changeKeys()
	{
		
	}
}
