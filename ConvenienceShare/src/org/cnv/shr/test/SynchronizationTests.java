package org.cnv.shr.test;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.Misc;
import org.junit.Assert;
import org.junit.Test;

public class SynchronizationTests extends RemotesTest
{
	public void assertFileInDb(String localDir, File f)
	{
		
	}
	public void assertFileNotInDb(String localDir, File f)
	{
		
	}
	
	@Test
	public void testLocalSync() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);)
		{
			Path createTempDirectory = Files.createTempDirectory("root");
			String path = createTempDirectory.toFile().getAbsolutePath();
			String rootName = Misc.getRandomString(15);
			
			LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024, 50);
			
			UserActions.addLocal(createTempDirectory.toFile(), false, rootName);
			Thread.sleep(1000);
			Assert.assertNotNull(DbRoots.getLocal(path));
			UserActions.sync(DbRoots.getLocal(path));
			
			long diskSpace = TestUtils.sum(makeSampleDirectories);
			LocalDirectory local = DbRoots.getLocal(path);
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
			UserActions.addMachine(getMachineInfo(0).getUrl());
			Thread.sleep(1000);
			
			Path createTempDirectory = Files.createTempDirectory("root");
			String path = createTempDirectory.toFile().getAbsolutePath();
			String rootName = Misc.getRandomString(15);
			
			LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024, 114);
			getMachineInfo(0).send(new TestActions.ADD_LOCAL(createTempDirectory.toFile().getAbsolutePath(), rootName));
			Thread.sleep(5000);
			
			Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
			Assert.assertNotNull(machine);
			UserActions.syncRoots(machine);
			
			Thread.sleep(5000);
			Assert.assertNotNull(DbRoots.getRoot(machine, rootName));
			
			UserActions.syncRemote(DbRoots.getRoot(machine, rootName));
			Thread.sleep(5000);

			long diskSpace = TestUtils.sum(makeSampleDirectories);
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
				
				Path createTempDirectory = Files.createTempDirectory("root");
				String path = createTempDirectory.toFile().getAbsolutePath();
				String rootName = Misc.getRandomString(15);
				
				LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024, 114);
				UserActions.addLocal(createTempDirectory.toFile(), true, rootName);
				Thread.sleep(5000);
				
				getMachineInfo(0).send(new TestActions.SYNC_ROOTS(Services.localMachine.getIdentifier()));
				Thread.sleep(2000);
				getMachineInfo(0).send(new TestActions.SYNC_REMOTE(Services.localMachine.getIdentifier(), rootName));
				Thread.sleep(2000);
				
				Machine machine = DbMachines.getMachine(getMachineInfo(0).getIdent());
				Assert.assertNotNull(machine);
				
				// TODO: asserts...
				
				Thread.sleep(5000);
			}
		}
}
