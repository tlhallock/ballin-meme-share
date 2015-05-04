package org.cnv.shr.lcl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;

public class LocalDirectorySyncIterator
{
	private SynchronizationTask next;
	private LinkedList<Node> stack = new LinkedList<>();
	private LocalDirectory root;
		
		
		public LocalDirectorySyncIterator(LocalDirectory directory)
		{
			String path = directory.getCanonicalPath();
			root = directory;
			File f = new File(path);

			if (Files.isSymbolicLink(Paths.get(f.getAbsolutePath()))
				|| !f.isDirectory())
			{
				throw new RuntimeException("Symbolic link: " + directory + ". Skipping");
			}

			PathElement pathElement = DbPaths.getPathElement(root, path);
			if (pathElement == null)
			{
				throw new RuntimeException("Unable to get path of " + path);
			}
			stack.addLast(new Node(new Pair(f, pathElement), new SynchronizationTask(
					pathElement.getId(), root, f.listFiles(), DbPaths.ROOT)));
			
			
//			if (Files.isSymbolicLink(Paths.get(directory.getAbsolutePath())))
//			{
//				throw new RuntimeException("Symbolic link: " + directory + ". Skipping");
//			}
//			else if (directory.isDirectory())
//			{
//				File[] files = directory.listFiles();
//				if (files == null)
//				{
//					throw new RuntimeException("Unable to create a file iterator of file " + directory);
//				}
//				Arrays.sort(files, Find.FILE_COMPARATOR);
//				stack.addLast(new Node(directory, files));
//				next = stack.getLast().findNext();
//			}
//			else
//			{
//				throw new RuntimeException("Unable to create a file iterator of file " + directory);
//			}
		}

		public SynchronizationTask next()
		{
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
					File childFile = sync.synchronizedResults[index].fsCopy;
					PathElement dbDir = sync.synchronizedResults[index].dbCopy;
					if (!childFile.exists())
					{
						// make sure it is not in the database...
						index++;
						continue;
					}
					File[] grandChildren = childFile.listFiles();
					if (grandChildren == null)
					{
						index++;
						continue;
					}
					
					stack.addLast(new Node(sync.synchronizedResults[index], new SynchronizationTask(
							this.current.dbCopy.getId(), root, grandChildren, dbDir)));
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
