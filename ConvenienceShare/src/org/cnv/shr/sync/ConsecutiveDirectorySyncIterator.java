
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


package org.cnv.shr.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.FileSource.FileSourceIterator;
import org.cnv.shr.util.LogWrapper;

public class ConsecutiveDirectorySyncIterator extends SyncrhonizationTaskIterator
{
	private boolean first;
	private LinkedList<Node> stack = new LinkedList<>();
	private RootDirectory root;
		
	public ConsecutiveDirectorySyncIterator(final RootDirectory remoteDirectory, final FileSource f) throws IOException
	{
		root = remoteDirectory;
		stack.addLast(new Node(new SynchronizationTask(DbPaths.ROOT, root, f.listFiles())));
		first = true;
	}

	@Override
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

		Node(final SynchronizationTask sync)
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
				final FileSource childFile = sync.synchronizedResults[index].getFsCopy();
				final PathElement dbDir = sync.synchronizedResults[index].getPathElement();
				if (!childFile.stillExists())
				{
					// make sure it is not in the database...
					index++;
					continue;
				}
				
				try (FileSourceIterator grandChildren = childFile.listFiles();)
				{
					stack.addLast(new Node(new SynchronizationTask(dbDir, root, grandChildren)));
					index++;
					return true;
				}
				catch (final IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to create synchronization node.", e);
					index++;
					continue;
				}
			}
			final Node last = stack.removeLast();
			if (!last.equals(this))
			{
				throw new RuntimeException("This should be the last element on the stack.");
			}
			return false;
		}
	}
}
