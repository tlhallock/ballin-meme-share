package org.cnv.shr.test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AddMachine;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.ExplorerSyncIterator;
import org.cnv.shr.sync.FileFileSource;
import org.cnv.shr.sync.FileSource;
import org.cnv.shr.sync.LocalSynchronizer;
import org.cnv.shr.sync.Pair;
import org.cnv.shr.sync.RemoteFileSource;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;
import org.cnv.shr.util.Misc;
import org.junit.Assert;
import org.junit.Test;

public class SynchronizationTests extends RemotesTest
{
	public void assertFileInDb(final String localDir, final File f)
	{
		
	}
	public void assertFileNotInDb(final String localDir, final File f)
	{
		
	}
	
	@Test
	public void testLocalSync() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);)
		{
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			final LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024, 50);
			
			UserActions.addLocal(createTempDirectory.toFile(), false, rootName);
			Thread.sleep(1000);
			Assert.assertNotNull(DbRoots.getLocal(path));
			UserActions.sync(DbRoots.getLocal(path));
			
			final long diskSpace = TestUtils.sum(makeSampleDirectories);
			final LocalDirectory local = DbRoots.getLocal(path);
			Assert.assertEquals(diskSpace, local.diskSpace());
			Assert.assertEquals(makeSampleDirectories.size(), local.numFiles());
			Misc.rm(createTempDirectory);
		}
	}
	
	@Test
	public void testSyncToRemote() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);
			 Closeable c1 = getMachineInfo(0).launch(true))
		{
			UserActions.addMachine(getMachineInfo(0).getUrl(), new AddMachine.AddMachineParams(true));
			Thread.sleep(1000);
			
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			final LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024, 114);
			getMachineInfo(0).send(new TestActions.ADD_LOCAL(createTempDirectory.toFile().getAbsolutePath(), rootName));
			Thread.sleep(5000);
			
			getMachineInfo(0).send(new TestActions.SHARE_WITH(Services.localMachine.getIdentifier(), true));
			
			final Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
			Assert.assertNotNull(machine);
			UserActions.syncRoots(machine);
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			

			ReentrantLock reentrantLock = new ReentrantLock();
			reentrantLock.lock();
			reentrantLock.newCondition().await();
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			Thread.sleep(5000);
			Assert.assertNotNull(DbRoots.getRoot(machine, rootName));
			DbRoots.getRoot(machine, rootName).synchronize(null);

			final long diskSpace = TestUtils.sum(makeSampleDirectories);
			Assert.assertEquals(diskSpace, DbRoots.getRoot(machine, rootName).diskSpace());
			Assert.assertEquals(makeSampleDirectories.size(), DbRoots.getRoot(machine, rootName).numFiles());
			Misc.rm(createTempDirectory);
		}
	}
	
	@Test
	public void testSyncToRemoteExplorer() throws Exception
	{
		try (final Closeable c2 = launchLocalMachine(true);
			 final Closeable c1 = getMachineInfo(0).launch(true))
		{
			UserActions.addMachine(getMachineInfo(0).getUrl(), new AddMachine.AddMachineParams(true));
			Thread.sleep(1000);
			
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			final LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 4, 10, 1024, 114);
			getMachineInfo(0).send(new TestActions.ADD_LOCAL(createTempDirectory.toFile().getAbsolutePath(), rootName));
			Thread.sleep(5000);
			
			final Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
			Assert.assertNotNull(machine);
			UserActions.syncRoots(machine);
			
			Thread.sleep(5000);
			Assert.assertNotNull(DbRoots.getRoot(machine, rootName));

			final RootDirectory rootDirectory = DbRoots.getRoot(machine, rootName);
			final ExplorerSyncIterator iterator = new ExplorerSyncIterator(rootDirectory);
			FileSource rootSource;
			RootSynchronizer synchronizer;
			if (rootDirectory.isLocal())
			{
				rootSource = new FileFileSource(new File(rootDirectory.getPathElement().getFullPath()),
										DbRoots.getIgnores((LocalDirectory) rootDirectory));
				synchronizer = new LocalSynchronizer((LocalDirectory) rootDirectory, iterator);
			}
			else
			{
				final RemoteSynchronizerQueue createRemoteSynchronizer = Services.syncs.createRemoteSynchronizer((RemoteDirectory) rootDirectory);
				rootSource = new RemoteFileSource((RemoteDirectory) rootDirectory, createRemoteSynchronizer);
				iterator.setCloseable(createRemoteSynchronizer);
				synchronizer = new RemoteSynchronizer((RemoteDirectory) rootDirectory, iterator);
			}
			
			class Listener implements TaskListener
			{
				boolean fail;
				long activity = System.currentTimeMillis();
				
				@Override
				public void syncCompleted(final Pair[] pairs)
				{
					activity = System.currentTimeMillis();
					for (final Pair pair : pairs)
					{
						try
						{
							System.out.println("Queuing " + pair.getSource().getCanonicalPath());
							System.out.println("and     " + pair.getPathElement().getFullPath());
							iterator.queueSyncTask(pair.getSource(), pair.getPathElement(), this);
						}
						catch (final IOException e)
						{
							fail = true;
							Services.logger.print(e);
						}
					}
				}
			};
			
			final Listener listener = new Listener();
			iterator.queueSyncTask(rootSource, DbPaths.ROOT, listener);
			
			// meant to be killed forcefully...
			new Thread(synchronizer).start();
			while (listener.activity > System.currentTimeMillis() - 2000)
			{
				Thread.sleep(2000);
			}
			Assert.assertFalse(listener.fail);
			DbRoots.getRoot(machine, rootName).setStats();
			
			final long diskSpace = TestUtils.sum(makeSampleDirectories);
			Assert.assertEquals(diskSpace, DbRoots.getRoot(machine, rootName).diskSpace());
			Assert.assertEquals(makeSampleDirectories.size(), DbRoots.getRoot(machine, rootName).numFiles());
			Misc.rm(createTempDirectory);
		}
	}

	@Test
	public void testOtherSync() throws Exception
	{
		try (    Closeable c2 = launchLocalMachine(true);
				 Closeable c1 = getMachineInfo(0).launch(true))
		{
			getMachineInfo(0).send(new TestActions.AddMachine(getLocalUrl()));
			Thread.sleep(1000);
			
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			final LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024, 114);
			UserActions.addLocal(createTempDirectory.toFile(), true, rootName);
			Thread.sleep(5000);
			
			getMachineInfo(0).send(new TestActions.SYNC_ROOTS(Services.localMachine.getIdentifier()));
			Thread.sleep(2000);
			getMachineInfo(0).send(new TestActions.SYNC_REMOTE(Services.localMachine.getIdentifier(), rootName));
			Thread.sleep(2000);
			
			final Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
			Assert.assertNotNull(machine);
			
			// TODO: asserts...
			
			Thread.sleep(5000);
		}
	}
}
