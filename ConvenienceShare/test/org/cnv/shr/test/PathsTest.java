package org.cnv.shr.test;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.junit.Assert;
import org.junit.Test;

public class PathsTest extends LocalMachineTest
{
	@Test
	public void testPaths() throws IOException, Exception
	{
		try (Closeable c2 = launchLocalMachine(true);)
		{
			final Path createTempDirectory = Files.createTempDirectory("root");
			final String path = createTempDirectory.toFile().getAbsolutePath();
			final String rootName = Misc.getRandomString(15);
			
			UserActions.addLocalImmediately(createTempDirectory, rootName);
			final LocalDirectory local = DbRoots.getLocal(path);
			Assert.assertNotNull(local);

			String[] testPaths = new String[] {
					// Just can't start with same path element right now...
					"ala;lskdjf;als0123456789 0123456789kdjf;lkj+.a.b.v.d",
					"b/b",
					"c/ala;lskdjf;alskdjf01234567890123456789;lkj+.a.b/a.f.d01234567890123456789.v.d",
					"d/e/f/g/h/i/j/k/l/",
			};
			
			for (String testPath : testPaths)
			{
				PathElement pathElement = DbPaths.getPathElement(testPath);
				DbPaths.pathLiesIn(pathElement, local);

				LogWrapper.getLogger().info("Added " + testPath);
				// DbObjects.PELEM.debug(Services.h2DbCache.getThreadConnection());
				// DbObjects.ROOT_CONTAINS.debug(Services.h2DbCache.getThreadConnection());

				ensurePathExistsFromTop(local, testPath);
				ensurePathExistsFromBottom(local, testPath);
			}
			
			// These are obviously not being deleted.
			Misc.rm(createTempDirectory);
		}
	}

	private void ensurePathExistsFromTop(LocalDirectory local, String testPath)
	{
		PathElement current = DbPaths.ROOT;
		String remaining = testPath;
		
		outer: while (remaining.length() > 0)
		{
			try (DbIterator<PathElement> listPathElements = DbPaths.listPathElements(local, current);)
			{
				while (listPathElements.hasNext())
				{
					PathElement child = listPathElements.next();
					if (!remaining.startsWith(child.getName()))
					{
						continue;
					}

					remaining = remaining.substring(child.getName().length());
					current = child;
					continue outer;
				}
			}

			Assert.fail("Path not found at " + remaining);
		}
	}

	private void ensurePathExistsFromBottom(LocalDirectory local, String testPath)
	{
		// something.list(local); should have pathElement
	}
}
