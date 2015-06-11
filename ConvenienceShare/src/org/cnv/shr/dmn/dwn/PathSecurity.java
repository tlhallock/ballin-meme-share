
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */


package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class PathSecurity
{
	static Path secureMakeDirs(Path rootFile, Path path)
	{
		Path root;
		Misc.ensureDirectory(rootFile, false);
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
}
