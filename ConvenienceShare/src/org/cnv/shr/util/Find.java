package org.cnv.shr.util;

import java.io.File;

public class Find
{
	public interface FileListener
	{
		void fileFound(File f);
		void flush();
	}
	
	public static void find(File directory, Find.FileListener listener)
	{
		findInternal(directory, listener);
		listener.flush();
	}
	
	private static void findInternal(File directory, Find.FileListener listener)
	{
		if (directory.isDirectory())
		{
			File[] children = directory.listFiles();
			if (children == null)
			{
				return;
			}
			for (File child : children)
			{
				find(child, listener);
			}
		}
		else if (directory.isFile())
		{
			listener.fileFound(directory);
		}
	}
}
