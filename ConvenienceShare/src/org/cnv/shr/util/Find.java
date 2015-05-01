package org.cnv.shr.util;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class Find implements Iterator<File>
{
	private File next;
	private LinkedList<Node> stack = new LinkedList<>();
	
	public Find(File directory)
	{
		if (directory.isFile())
		{
			stack.addLast(new Node(new File[]{ directory }));
		}
		else if (directory.isDirectory())
		{
			File[] files = directory.listFiles();
			if (files == null)
			{
				throw new RuntimeException("Unable to create a file iterator of file " + directory);
			}
			Arrays.sort(files, FILE_COMPARATOR);
			stack.addLast(new Node(files));
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
		next = stack.getLast().findNext();
		return returnValue;
	}

	@Override
	public boolean hasNext()
	{
		return next != null;
	}

	private class Node
	{
		private File[] files;
		private int index;
		
		Node(File[] files)
		{
			this.files = files;
		}
		
		private File findNext()
		{
			while (index < files.length - 1)
			{
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
				Node node = new Node(children);
				Arrays.sort(children, FILE_COMPARATOR);
				stack.add(node);
				File c = stack.getLast().findNext(); 
				if (c != null)
				{
					index++;
					return c;
				}
			}
			if (!stack.removeLast().equals(this))
			{
				throw new RuntimeException("This should be the last element on the stack.");
			}
			if (stack.isEmpty())
			{
				return null;
			}
			return stack.getLast().findNext();
		}
	}
	
	private Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
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
