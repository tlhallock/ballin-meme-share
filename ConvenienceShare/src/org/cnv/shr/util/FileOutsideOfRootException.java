package org.cnv.shr.util;

import java.io.IOException;
import java.nio.file.Path;

public class FileOutsideOfRootException extends IOException
{
	public FileOutsideOfRootException(Path dir, String file)
	{
		super("Root = " + dir + " file = " + file);
	}

	public FileOutsideOfRootException(String dir, Path file)
	{
		super("Root = " + dir + " file = " + file);
	}
}
