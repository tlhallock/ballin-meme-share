package org.cnv.shr.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileFilter
{
	Pattern p;
	
	public FileFilter(Pattern p)
	{
		this.p = p;
	}
	
	public boolean accept(File f)
	{
		String path;
		
		try
		{
			path = f.getCanonicalPath();
		}
		catch (IOException e)
		{
			path = f.getAbsolutePath();
		}
		return p.matcher(path).find();
	}
}
