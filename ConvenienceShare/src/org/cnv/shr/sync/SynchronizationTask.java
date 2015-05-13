package org.cnv.shr.sync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class SynchronizationTask
{
	ArrayList<FileSource> files = new ArrayList<>();
	LinkedList<PathElement> dbPaths;
	LinkedList<TaskListener> listeners = new LinkedList<>();
	
	Pair<? extends FileSource>[] synchronizedResults;
	
	PathElement current;
	
	SynchronizationTask(PathElement current, RootDirectory local, Iterator<FileSource> grandChildren)
	{
		this.current = current;
		dbPaths = current.list(local);
		while (grandChildren.hasNext())
		{
			files.add(grandChildren.next());
		}
	}
	
	public void addListener(TaskListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(TaskListener listener)
	{
		listeners.remove(listener);
	}

	public void setResults(Pair[] array)
	{
		synchronizedResults = array;
		for (TaskListener listener : listeners)
		{
			listener.syncCompleted();
		}
	}
	
	public interface TaskListener
	{
		public void syncCompleted();
	}

	public Pair<? extends FileSource>[] getSynchronizationResults()
	{
		return synchronizedResults;
	}
}
