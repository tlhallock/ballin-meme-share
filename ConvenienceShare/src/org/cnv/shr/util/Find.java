
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



package org.cnv.shr.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class Find implements Iterator<File>
{
	private File next;
	private LinkedList<Node> stack = new LinkedList<>();

	public Find(String string)
	{
		this(new File(string));
	}
	
	public Find(File directory)
	{
		if (Files.isSymbolicLink(Paths.get(directory.getAbsolutePath())))
		{
			throw new RuntimeException("Symbolic link: " + directory + ". Skipping");
		}
		if (directory.isFile())
		{
			stack.addLast(new Node(null, new File[]{ directory }));
		}
		else if (directory.isDirectory())
		{
			File[] files = directory.listFiles();
			if (files == null)
			{
				throw new RuntimeException("Unable to create a file iterator of file " + directory);
			}
			Arrays.sort(files, FILE_COMPARATOR);
			stack.addLast(new Node(directory, files));
			next = stack.getLast().findNext();
		}
		else
		{
			throw new RuntimeException("Unable to create a file iterator of file " + directory);
		}
	}

	@Override
	public File next()
	{
		File returnValue = next;
		do
		{
			next = stack.getLast().findNext();
		}
		while (next == null && !stack.isEmpty());
		return returnValue;
	}

	@Override
	public boolean hasNext()
	{
		return next != null;
	}

	private class Node
	{
		private File file;
		private File[] files;
		private int index;
		
		Node(File file, File[] files)
		{
			this.file = file;
			this.files = files;
		}
		
		
		private File findNext()
		{
			while (index < files.length)
			{
				if (Files.isSymbolicLink(Paths.get(files[index].getAbsolutePath())))
				{
					LogWrapper.getLogger().info("Skipping symbolic link: " + files[index]);
					
					index++;
					continue;
				}
				if (files[index].isFile())
				{
					return files[index++];
				}
				if (!files[index].isDirectory())
				{
					index++;
					continue;
				}
				File[] children = files[index].listFiles();
				if (children == null)
				{
					index++;
					continue;
				}
				
				Node node = new Node(files[index], children);
				Arrays.sort(children, FILE_COMPARATOR);
				stack.addLast(node);
				index++;

				File c = node.findNext(); 
				if (c != null)
				{
					return c;
				}
			}
			Node last = stack.removeLast();
			if (!last.equals(this))
			{
				throw new RuntimeException("This should be the last element on the stack.");
			}
			return null;
		}
	}
	
	static Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
		@Override
		public int compare(File arg0, File arg1)
		{
			return arg0.getAbsolutePath().compareTo(arg1.getAbsolutePath());
		}};

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("Cannot remove.");
	}
}
