package org.cnv.shr.gui;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
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
			public void run()
			{
				DbMachines.delete(remote);
				Services.notifications.remotesChanged();
			}
		});
	}

	public static void addMachine(final String url)
	{
		Services.userThreads.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					Communication openConnection = Services.networkManager.openConnection(url, true);
					if (openConnection == null)
					{
						Services.keyManager.addPendingAuthentication(url);
						return;
					}
					openConnection.send(new MachineFound());
					openConnection.send(new FindMachines());
					openConnection.notifyDone();
				}
				catch (IOException e)
				{
					Services.logger.logStream.println("Unable to discover " + url);
					e.printStackTrace(Services.logger.logStream);
				}
			}
		});
	}

	public static void syncRoots(final Machine m)
	{
		Services.userThreads.execute(new Runnable()
		{
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
					openConnection.notifyDone();
				}
				catch (IOException e)
				{
					Services.logger.logStream.println("Unable to discover " + url);
					e.printStackTrace(Services.logger.logStream);
				}
			}
		});
	}

	public static void findMachines(final Machine m)
	{
		Services.userThreads.execute(new Runnable()
		{
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
					openConnection.notifyDone();
				}
				catch (IOException e)
				{
					Services.logger.logStream.println("Unable to discover refresh " + m.getUrl());
					e.printStackTrace(Services.logger.logStream);
				}
			}
		});
	}

	public static void syncAllLocals()
	{
		Services.userThreads.execute(new Runnable()
		{
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
			}
		});
	}

	public static void syncRemote(final RootDirectory directory)
	{
		Services.userThreads.execute(new Runnable()
		{
			public void run()
			{
				directory.synchronize(null);
			}
		});
	}

	public static void addLocal(final File localDirectory, final boolean sync)
	{
		Services.userThreads.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					Services.logger.logStream.println("Sharing " + localDirectory);
					LocalDirectory local = DbRoots.getLocal(localDirectory.getCanonicalPath());
					if (sync)
					{
						sync(local);
					}
				}
				catch (IOException e1)
				{
					Services.logger.logStream.println("Unable to get file path to share: " + localDirectory);
					e1.printStackTrace(Services.logger.logStream);
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
					e.printStackTrace();
				}
			}
		});
	}

	public static void debug()
	{
		Services.userThreads.execute(new Runnable()
		{
			public void run()
			{
				DbTables.debugDb(Services.logger.logStream);
			}
		});
	}

	public static void deleteDb()
	{
		Services.userThreads.execute(new Runnable()
		{
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
