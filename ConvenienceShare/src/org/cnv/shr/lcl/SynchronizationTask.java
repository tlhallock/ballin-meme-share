package org.cnv.shr.lcl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;

public class SynchronizationTask
{
	ArrayList<File> files = new ArrayList<>();
	LinkedList<PathElement> dbPaths;
	
	Pair[] synchronizedResults;
	
	PathElement current;
	
	SynchronizationTask(PathElement current, LocalDirectory local, File[] listed)
	{
		this.current = current;
		dbPaths = current.list(local);
		for (File f : listed)
			files.add(f);
//		split(listed);
	}

//	private void split(File[] listed)
//	{
//		for (File child : listed)
//		{
//			if (Files.isSymbolicLink(Paths.get(child.getAbsolutePath())))
//			{
//				Services.logger.logStream.println("Skipping symbolic link: " + child.getAbsolutePath());
//			}
//			else if (child.isFile())
//			{
//				files.add(child);
//			}
//			else if (child.isDirectory())
//			{
//				subDirectories.add(child);
//			}
//		}
//	}
}
