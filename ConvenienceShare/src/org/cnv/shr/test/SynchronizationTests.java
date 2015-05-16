package org.cnv.shr.test;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalDirectory;
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
	public void testOtherAdd() throws Exception
	{
		try (Closeable c2 = launchLocalMachine();)
		{
			Path createTempDirectory = Files.createTempDirectory("root");
			String path = createTempDirectory.toFile().getAbsolutePath();
			
			LinkedList<File> makeSampleDirectories = TestUtils.makeSampleDirectories(path, 3, 10, 1024 * 1024, 114);
			
			UserActions.addLocal(createTempDirectory.toFile(), false);
			UserActions.sync(DbRoots.getLocal(path));
			
			Thread.sleep(10000);

			long diskSpace = TestUtils.sum(makeSampleDirectories);
			LocalDirectory local = DbRoots.getLocal(path);
			Assert.assertEquals(diskSpace, local.diskSpace());
			Assert.assertEquals(makeSampleDirectories.size(), local.numFiles());
			Misc.rm(createTempDirectory);
		}
	}
}
