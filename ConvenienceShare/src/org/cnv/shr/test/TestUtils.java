package org.cnv.shr.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import org.cnv.shr.util.Misc;

public class TestUtils
{
	protected static final Random RANDOM = new Random();
//	private static final long FILE_SIZE = 1024;
//	private static final int FILES_PER_DIRECTORY = 10;
	
	public static void assertFilesAreEqual(File f1, File f2)
	{
		
	}
	
	public static LinkedList<File> makeSampleDirectories(String root, int depth, int filesPerDirectory, long fileSize) throws IOException
	{
		LinkedList<File> list = new LinkedList<File>();
		makeSampleDirectory(root, 0, depth, filesPerDirectory, fileSize, list);
		return list;
	}
	public static void makeSampleDirectory(String root, int depth, int maxDepth, int filesPerDirectory, long fileSize, LinkedList<File> returnValue) throws IOException
	{
		if (depth >= maxDepth)
		{
			return;
		}
		int numToCreate = RANDOM.nextInt(filesPerDirectory);
		for (int i = 0; i < numToCreate; i++)
		{
			returnValue.add(createFile(root + File.separator + Misc.getRandomString(15) + "." + Misc.getRandomString(3), RANDOM.nextInt((int) fileSize)));
		}
		int numDirsToCreate = RANDOM.nextInt(filesPerDirectory);
		for (int i = 0; i < numDirsToCreate; i++)
		{
			String path = root + File.separator + Misc.getRandomString(5);
			Misc.ensureDirectory(path, false);
			makeSampleDirectory(path, depth + 1, maxDepth, filesPerDirectory, fileSize, returnValue);
		}
	}

	public static File createFile(String path, long length) throws IOException
	{
		Misc.ensureDirectory(path, true);
		File file = new File(path);
		try (FileOutputStream fileOutputStream = new FileOutputStream(file);)
		{
			byte[] bytes = new byte[1];
			for (long i = 0; i < length; i++)
			{
				RANDOM.nextBytes(bytes);
				fileOutputStream.write(bytes);
			}
		}
		return file;
	}
}
