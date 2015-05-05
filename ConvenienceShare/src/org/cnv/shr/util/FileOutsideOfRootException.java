package org.cnv.shr.util;

import java.io.IOException;

public class FileOutsideOfRootException extends IOException
{
	public FileOutsideOfRootException(String root, String file)
	{
		super("Root = " + root + " file = " + file);
	}
}
