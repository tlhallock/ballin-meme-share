package org.cnv.shr.test;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.not.NotificationListenerAdapter;
import org.cnv.shr.gui.AddMachine;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.Misc;
import org.junit.Assert;
import org.junit.Test;

public class DownloadTest extends RemotesTest
{
	// Assert the notifications too...
	@Test
	public void testDownloadFromRemote() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);
			 Closeable c1 = getMachineInfo(0).launch(true))
		{
			UserActions.addMachine(getMachineInfo(0).getUrl(), new AddMachine.AddMachineParams(true));
			Thread.sleep(1000);
			
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			final LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 4, 2, 1024 * 1024, 5);
			getMachineInfo(0).send(new TestActions.ADD_LOCAL(createTempDirectory.toFile().getAbsolutePath(), rootName));
			Thread.sleep(5000);
			
			final Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
			Assert.assertNotNull(machine);
			UserActions.syncRoots(machine);
			
			Thread.sleep(5000);
			Assert.assertNotNull(DbRoots.getRoot(machine, rootName));
			DbRoots.getRoot(machine, rootName).synchronize(null);
			
			// Sometimes makeSampleDirectories is empty... oops.
			File makeFile = makeSampleDirectories.get((int) (Math.random() * makeSampleDirectories.size()));
			Path path2 = Paths.get(makeFile.getAbsolutePath());
			Path relativize = createTempDirectory.relativize(path2);
			PathElement pathElement = DbPaths.getPathElement(DbPaths.ROOT, relativize.toString());
			SharedFile file = DbFiles.getFile(DbRoots.getRoot(machine, rootName), pathElement);

			class MyNotificationListener extends NotificationListenerAdapter
			{
				boolean done;
				boolean fail;
				@Override
				public void downloadRemoved(DownloadInstance d)
				{
					done = true;
					fail = true;
					System.out.println("Done with download.");
				}
				
				@Override
				public void downloadDone(DownloadInstance d)
				{
					done = true;
					System.out.println("Done with download.");
				}
			}
			MyNotificationListener listener = new MyNotificationListener();
			Services.notifications.add(listener);
			DownloadInstance download = Services.downloads.download(file);

			while (!listener.done)
			{
				Thread.sleep(1000);
			}
			
			Assert.assertFalse(listener.fail);
			
			File destinationFile = download.getDestinationFile().toFile();
			TestUtils.assertFilesAreEqual(makeFile, destinationFile);
		}
	}
	
	@Test
	public void testServeToRemote() throws Exception
	{
		try (    Closeable c2 = launchLocalMachine(true);
				 Closeable c1 = getMachineInfo(0).launch(true))
		{
			getMachineInfo(0).send(new TestActions.AddMachine(getLocalUrl()));
			Thread.sleep(1000);
			
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			final LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 4, 2, 1024 * 1024, 5);
			UserActions.addLocalImmediately(createTempDirectory, rootName);
			DbRoots.getLocalByName(rootName).synchronize(null);
			
			Thread.sleep(5000);
			
			getMachineInfo(0).send(new TestActions.SYNC_ROOTS(Services.localMachine.getIdentifier()));
			Thread.sleep(2000);
			getMachineInfo(0).send(new TestActions.SYNC_REMOTE(Services.localMachine.getIdentifier(), rootName));
			Thread.sleep(2000);
			
			final Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
			Assert.assertNotNull(machine);

			File makeFile = makeSampleDirectories.get((int) (Math.random() * makeSampleDirectories.size()));
			Path path2 = Paths.get(makeFile.getAbsolutePath());
			Path relativize = createTempDirectory.relativize(path2);
			
			getMachineInfo(0).send(new TestActions.DOWNLOAD(Services.localMachine.getIdentifier(), rootName, relativize.toString()));
			
			Thread.sleep(5000);
			System.out.println("Break here...");
		}
	}
}
