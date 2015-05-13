package org.cnv.shr.sync;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class ConsecutiveDirectorySyncIterator implements SyncrhonizationTaskIterator
{
	private boolean first;
	private LinkedList<Node> stack = new LinkedList<>();
	private RootDirectory root;
	FileSource source;
		
	public ConsecutiveDirectorySyncIterator(RootDirectory remoteDirectory, FileSource f) throws IOException
	{
		root = remoteDirectory;
		source = f;
		stack.addLast(new Node(new SynchronizationTask(DbPaths.ROOT, root, f.listFiles())));
		first = true;
	}

	public SynchronizationTask next()
	{
		if (first)
		{
			first = false;
			return stack.getLast().sync;
		}
		do
		{
			if (stack.isEmpty())
			{
				return null;
			}
		} while (!stack.getLast().findNext());
		return stack.getLast().sync;
	}

	public class Node
	{
		private SynchronizationTask sync;
		private int index = 0;

		Node(SynchronizationTask sync)
		{
			this.sync = sync;
			this.index = 0;

			// Collections.sort(files, Find.FILE_COMPARATOR);
			// Collections.sort(subDirectories, Find.FILE_COMPARATOR);
		}

		private boolean findNext()
		{
			while (index < sync.synchronizedResults.length)
			{
				FileSource childFile = sync.synchronizedResults[index].getFsCopy();
				PathElement dbDir = sync.synchronizedResults[index].getPathElement();
				if (!childFile.stillExists())
				{
					// make sure it is not in the database...
					index++;
					continue;
				}
				Iterator<FileSource> grandChildren;
				try
				{
					grandChildren = childFile.listFiles();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					index++;
					continue;
				}
				if (grandChildren == null)
				{
					index++;
					continue;
				}

				stack.addLast(new Node(new SynchronizationTask(dbDir, root, grandChildren)));
				index++;
				return true;
			}
			Node last = stack.removeLast();
			if (!last.equals(this))
			{
				throw new RuntimeException("This should be the last element on the stack.");
			}
			return false;
		}
	}

	@Override
	public void close() throws IOException
	{
		source.close();
	}
}
