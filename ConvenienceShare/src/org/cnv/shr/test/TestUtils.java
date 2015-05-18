package org.cnv.shr.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Random;

import org.cnv.shr.util.Misc;
import org.junit.Assert;

public class TestUtils
{
	protected static final Random RANDOM = new Random();
//	private static final long FILE_SIZE = 1024;
//	private static final int FILES_PER_DIRECTORY = 10;
	
	public static void assertFilesAreEqual(File f1, File f2)
	{
		try (InputStream in1 = new BufferedInputStream(new FileInputStream(f1));
			 InputStream in2 = new BufferedInputStream(new FileInputStream(f2));)
		{
			int readByte;
			do
			{
				readByte = in1.read();
				Assert.assertEquals(readByte, in2.read());
			} while (readByte >= 0);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.fail("Had exception.");
		}
	}
	
	public static LinkedList<File> makeSampleDirectories(String root, int depth, int filesPerDirectory, long fileSize, int totalNumFiles) throws IOException
	{
		LinkedList<File> list = new LinkedList<File>();
		makeSampleDirectory(root, 0, depth, filesPerDirectory, fileSize, 0, totalNumFiles, list);
		return list;
	}
	public static void makeSampleDirectory(String root, 
			int depth, int maxDepth, 
			int filesPerDirectory, long fileSize,
			int totalFilesCreated, int totalNumFiles,
			LinkedList<File> returnValue) throws IOException
	{
		if (totalFilesCreated >= totalNumFiles || depth >= maxDepth)
		{
			return;
		}
		int numToCreate = Math.min(totalNumFiles - totalFilesCreated, RANDOM.nextInt(filesPerDirectory));
		for (int i = 0; i < numToCreate; i++)
		{
			String path = root + File.separator + Misc.getRandomString(15) + "." + Misc.getRandomString(3);
			System.out.println("Creating file " + path);
			returnValue.add(createFile(path, RANDOM.nextInt((int) fileSize)));
		}
		int numDirsToCreate = RANDOM.nextInt(filesPerDirectory);
		for (int i = 0; i < numDirsToCreate; i++)
		{
			String path = root + File.separator + Misc.getRandomString(5);
			System.out.println("Creating directory " + path);
			Misc.ensureDirectory(path, false);
			makeSampleDirectory(path, depth + 1, maxDepth, filesPerDirectory, fileSize, totalFilesCreated + numToCreate, totalNumFiles, returnValue);
		}
	}

	public static File createFile(String path, long length) throws IOException
	{
		Misc.ensureDirectory(path, true);
		File file = new File(path);
		try (OutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));)
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
	
	public static long sum(LinkedList<File> files)
	{
		long sum = 0;
		for (File f : files)
		{
			sum += f.length();
		}
		return sum;
	}
}
