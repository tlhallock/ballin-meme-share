package org.cnv.shr.lcl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;

public class DirectorySyncIterator
{
	private boolean first;
	private LinkedList<Node> stack = new LinkedList<>();
	private RootDirectory root;
		
	public DirectorySyncIterator(RootDirectory remoteDirectory, FileSource f) throws IOException
	{
		root = remoteDirectory;

		if (Files.isSymbolicLink(Paths.get(f.getCanonicalPath())) || !f.isDirectory())
		{
			try
			{
				remoteDirectory.delete();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			throw new RuntimeException("Symbolic link: " + remoteDirectory + ". Skipping");
		}

		stack.addLast(new Node(new Pair(f, DbPaths.ROOT), new SynchronizationTask(DbPaths.ROOT, root, f.listFiles())));
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
			}
			while (!stack.getLast().findNext())
				;
			return stack.getLast().sync;
		}
		
		public class Node
		{
			private Pair current;
			private SynchronizationTask sync;
			private int index = 0;
			
			Node(Pair current, SynchronizationTask sync)
			{
				this.current = current;
				this.sync = sync;
				this.index = 0;

//				Collections.sort(files, Find.FILE_COMPARATOR);
//				Collections.sort(subDirectories, Find.FILE_COMPARATOR);
				
				// make sure the path element is set.
			}
			
			private boolean findNext()
			{
				while (index < sync.synchronizedResults.length)
				{
					FileSource childFile = sync.synchronizedResults[index].getFsCopy();
					PathElement dbDir = sync.synchronizedResults[index].getPathElement();
					if (!childFile.exists())
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
					
					stack.addLast(new Node(sync.synchronizedResults[index],
							new SynchronizationTask(dbDir, root, grandChildren)));
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
}
