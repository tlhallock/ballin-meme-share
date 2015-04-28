package org.cnv.shr.util;

import java.io.File;

public class Find
{
	public static void find(File directory, FileListener listener)
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
	
	public interface FileListener
	{
		void fileFound(File f);
	}
}
