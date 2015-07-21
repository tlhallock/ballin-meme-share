
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.sync.FileSource.FileSourceIterator;

public class SynchronizationTask
{
	ArrayList<FileSource> files = new ArrayList<>();
	LinkedList<PathElement> dbPaths;
	LinkedList<TaskListener> listeners = new LinkedList<>();
	
	Pair[] synchronizedResults;
	
	PathElement current;
	
	SynchronizationTask(final PathElement current, final FileSourceIterator grandChildren)
	{
		this.current = current;
		dbPaths = current.list();
		
		while (grandChildren.hasNext())
		{
			files.add(grandChildren.next());
		}
	}
	
	public void addListener(final TaskListener listener)
	{
		if (listener == null) return;
		listeners.add(listener);
	}
	
	public void removeListener(final TaskListener listener)
	{
		listeners.remove(listener);
	}

	public void setResults(final Pair[] array)
	{
		synchronizedResults = array;
		Arrays.sort(synchronizedResults, new Comparator<Pair>() {
			@Override
			public int compare(Pair o1, Pair o2)
			{
				return PathElement.PATH_COMPARATOR.compare(o1.getPathElement(), o2.getPathElement());
			}});
		
		for (final TaskListener listener : listeners)
		{
			listener.syncCompleted(synchronizedResults);
		}
	}
	
	public interface TaskListener
	{
		public void syncCompleted(Pair[] pairs);
	}

	public Pair[] getSynchronizationResults()
	{
		return synchronizedResults;
	}
}
