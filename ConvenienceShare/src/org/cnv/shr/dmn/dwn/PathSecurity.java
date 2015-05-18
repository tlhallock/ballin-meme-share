package org.cnv.shr.dmn.dwn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.Misc;

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
		
		File next = new File(root);
		Iterator<String> iterator = pathElems.iterator();
		do
		{
			if (!next.exists())
			{
				next.mkdirs();
			}
			if (!next.isDirectory() || !isSecure(root, next))
			{
				return null;
			}
		} while (iterator.hasNext() && (next = new File(next.getPath() + File.separator + iterator.next())) != null);

		next = new File(next.getPath() + File.separator + filename);
		boolean alreadyExisted = next.exists();
		if (!alreadyExisted)
		{
			try
			{
				next.createNewFile();
			}
			catch (IOException e)
			{
				Services.logger.print(e);
				return null;
			}
		}
		if (!next.isFile() || !isSecure(root, next))
		{
			return null;
		}
		if (!alreadyExisted)
		{
			next.delete();
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
			Services.logger.print(e);
			return false;
		}
		if (!canonicalPath.startsWith(root))
		{
			return false;
		}
		return true;
	}

	public static String getFsName(String str)
	{
		StringBuilder builder = new StringBuilder(str.length());
		
		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			
			if (Character.isAlphabetic(c) 
					|| Character.isDigit(c) 
					|| c == '_' || c== '-' || c == '.' 
					|| c == '(' || c == ')')
			{
				builder.append(c);
				continue;
			}
			
			builder.append('[');
			builder.append( Misc.format(new byte[] { (byte) c}));
			builder.append(']');
		}
		
		return builder.toString();
	}
	
	public static File getMirrorDirectory(RemoteFile remoteFile)
	{
		RootDirectory rootDirectory = remoteFile.getRootDirectory();
		return new File(
				Services.settings.stagingDirectory.get() + File.separator
				  + getFsName(rootDirectory.getMachine().getName()) + "_"
				  + getFsName(rootDirectory.getMachine().getIdentifier()) + File.separator
				  + getFsName(rootDirectory.getName()) + File.separator);
	}
}
