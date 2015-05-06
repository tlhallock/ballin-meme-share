package org.cnv.shr.dmn.dwn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

class PathSecurity
{
	static File secureMakeDirs(File rootFile, String path)
	{
		String root;
		try
		{
			root = rootFile.getCanonicalPath();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return null;
		}
		
		LinkedList<String> pathElems = new LinkedList<>();
		for (String str : path.split("/"))
		{
			if (str.equals(".."))
			{
				return null;
			}
			pathElems.add(str);
		}
		
		String filename = pathElems.removeLast();
		
		File prev = new File(root);
		for (String pathElem : pathElems)
		{
			File next = new File(prev.getPath() + pathElem);
			if (!next.exists())
			{
				next.mkdirs();
			}
			if (!next.isDirectory() || !isSecure(root, next))
			{
				return null;
			}
		}

		File next = new File(prev.getPath() + filename);
		if (!next.exists())
		{
			try
			{
				next.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		if (!next.isFile() || !isSecure(root, next))
		{
			return null;
		}
		
		return next;
	}
	
	static boolean isSecure(String root, File file)
	{
		if (Files.isSymbolicLink(Paths.get(file.getAbsolutePath())))
		{
			return false;
		}
		String canonicalPath;
		try
		{
			canonicalPath = file.getCanonicalPath();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		if (!root.startsWith(canonicalPath))
		{
			return false;
		}
		return true;
	}
}
