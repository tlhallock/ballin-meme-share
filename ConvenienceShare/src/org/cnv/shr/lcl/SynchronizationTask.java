package org.cnv.shr.lcl;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;

public class SynchronizationTask
{
	ArrayList<File> files = new ArrayList<>();
	DbIterator<PathElement> dbPaths;
	
	Pair[] synchronizedResults;
	public int parentId;
	
	SynchronizationTask(int parentId, LocalDirectory local, File[] listed, PathElement parent)
	{
		this.parentId = parentId;
		dbPaths = DbPaths.listPathElements(local, parent);
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
