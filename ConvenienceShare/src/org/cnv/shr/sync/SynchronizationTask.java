package org.cnv.shr.sync;

import java.util.ArrayList;
import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.FileSource.FileSourceIterator;

public class SynchronizationTask
{
	ArrayList<FileSource> files = new ArrayList<>();
	LinkedList<PathElement> dbPaths;
	LinkedList<TaskListener> listeners = new LinkedList<>();
	
	Pair[] synchronizedResults;
	
	PathElement current;
	
	SynchronizationTask(final PathElement current, final RootDirectory local, final FileSourceIterator grandChildren)
	{
		this.current = current;
		dbPaths = current.list(local);
		
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
