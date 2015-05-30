package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class PathSecurity
{
	static Path secureMakeDirs(Path rootFile, Path path)
	{
		Path root;
		try
		{
			root = rootFile.normalize().toRealPath();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return null;
		}
		
		LinkedList<Path> pathElems = new LinkedList<>();
		for (Path str : path)
		{
			if (str.getFileName().equals(".."))
			{
				return null;
			}
			pathElems.add(str);
		}
		
		Path filename = pathElems.removeLast();
		
		Path next = root;
		Iterator<Path> iterator = pathElems.iterator();
		do
		{
			if (!Files.exists(next))
			{
				try
				{
					Files.createDirectories(next);
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to create parent directory: " + next, e);
					return null;
				}
			}
			if (!Files.isDirectory(next) || !isSecure(root, next))
			{
				return null;
			}
		} while (iterator.hasNext() && (next = next.resolve(iterator.next())) != null);

		// Somebody could create a file that looks like a symbolic link to this OS as the second to last path element.
		// We should create the file to check if it doesn't exist.
		next = next.resolve(filename);
		boolean alreadyExisted = Files.exists(next);
		if (!alreadyExisted)
		{
			try
			{
				Files.createFile(next);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to create file: " + next, e);
				return null;
			}
		}
		if (!Files.isRegularFile(next) || !isSecure(root, next))
		{
			return null;
		}
		if (!alreadyExisted)
		{
			try
			{
				Files.delete(next);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to delete test file: " + next, e);
			}
		}
		
		return next;
	}
	
	static boolean isSecure(Path root, Path file)
	{
		if (Files.isSymbolicLink(file))
		{
			return false;
		}
		file = file.normalize();
		if (!file.startsWith(root))
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
	
	public static Path getMirrorDirectory(RemoteFile remoteFile)
	{
		RootDirectory rootDirectory = remoteFile.getRootDirectory();
		return Paths.get(
				  Services.settings.stagingDirectory.get().getAbsolutePath(),
				  getFsName(rootDirectory.getMachine().getName()) + "_" + getFsName(rootDirectory.getMachine().getIdentifier()),
				  getFsName(rootDirectory.getName()));
	}
}
