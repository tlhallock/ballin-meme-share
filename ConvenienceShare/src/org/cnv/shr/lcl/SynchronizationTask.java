package org.cnv.shr.lcl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;

public class SynchronizationTask
{
	ArrayList<FileSource> files = new ArrayList<>();
	LinkedList<PathElement> dbPaths;
	
	Pair[] synchronizedResults;
	
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
}
