
package org.cnv.shr.lcl;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;

/**
 * I realize this would have been simpler with a method that recursively descended into subdirectories.
 * Instead, this keeps a stack inside the LocalDirectorySyncIterator of synchronization tasks to be performed.
 *
 */
public class LocalSynchronizer
{
	LocalDirectorySyncIterator iterator;
	LocalDirectory local;
	
	public LocalSynchronizer(LocalDirectory dir)
	{
		iterator = new LocalDirectorySyncIterator(dir);
	}
	
	public void run()
	{
		SynchronizationTask task = null;
		while ((task = iterator.next()) != null)
		{
			synchronize(task);
		}
	}
	
	public void synchronize(SynchronizationTask task)
	{
		HashMap<String, File> files = key(task.files);
		HashSet<String> accountedFor = new HashSet<>();
		
		LinkedList<Pair> subDirectories = new LinkedList<>();
		
		while (task.dbPaths.hasNext())
		{
			PathElement element = task.dbPaths.next();
			accountedFor.add(element.getName());
			
			File fsCopy = files.get(element.getName());
			LocalFile dbVersion = DbFiles.getFile(local, element);
			
			if (fsCopy == null)
			{
				if (dbVersion != null)
				{
					// delete stale file
					try
					{
						dbVersion.delete();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
				}
				
				try
				{
					// remove directory from database
					DbPaths.pathDoesNotLieIn(connection, element, local);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				if (fsCopy.isFile())
				{
					if (dbVersion == null)
					{
						// add File
						LocalFile lFile = new LocalFile(local, fsCopy);
						
						try
						{
							lFile.add(connection);
						}
						catch (SQLException e)
						{
							e.printStackTrace();
						}
					}
					else
					{
						// update file
						dbVersion.refreshAndWriteToDb();
					}
				}
				else if (fsCopy.isDirectory())
				{
					// nothing to be done
					Pair pair = new Pair();
					pair.dbCopy = element;
					pair.fsCopy = fsCopy;
					subDirectories.add(pair);
				}
			}
		}
		
		for (File f : files.values())
		{
			String name = f.getName();
			if (accountedFor.contains(f.getName()))
			{
				continue;
			}
			
			// add path to database
			PathElement element = new PathElement(task.parentId, name);
			try
			{
				element.add(connection);
				DbPaths.pathLiesIn(connection, element, local);
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
			}
			
			
			try
			{
				element.add(connection);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			if (f.isFile())
			{
				// add file
			}
			else
			{
				Pair pair = new Pair();
				pair.dbCopy = element;
				pair.fsCopy = f;
				subDirectories.add(pair);
			}
		}
		
//		LinkedList<SharedFile> toAdd = new LinkedList<>();
//		Find find = new Find(path);
//		while (find.hasNext())
//		{
//			File f = find.next();
//
//			String absolutePath;
//			try
//			{
//				absolutePath = f.getCanonicalPath();
//			}
//			catch (IOException e)
//			{
//				Services.logger.logStream.println("Unable to get file path: " + f);
//				e.printStackTrace(Services.logger.logStream);
//				continue;
//			}
//
//			if (Services.db.findLocalFile(this, f) != null)
//			{
//				continue;
//			}
//			Services.logger.logStream.println("Found file " + f);
//			try
//			{
//				toAdd.add(new LocalFile(getThis(), absolutePath));
//			}
//			catch(Exception ex)
//			{
//				Services.logger.logStream.println("Skipping file: " + f);
//				ex.printStackTrace(Services.logger.logStream);
//				continue;
//			}
//			changed = true;
//
//			if (toAdd.size() > 50)
//			{
//				Services.db.addFiles(this, toAdd);
//				toAdd.clear();
//
//				totalNumFiles = Services.db.countFiles(this);
//				totalFileSize = Services.db.countFileSize(this);
//				Services.db.updateDirectory(machine, this);
//				Services.notifications.localsChanged();
//			}
//		}
//		Services.db.addFiles(this, toAdd);
//		
//		
//		Services.logger.logStream.println("Synchronizing " + getCanonicalPath());
//
//		boolean changed = false;
//		changed |= prune();
//		changed |= search();
//
//		if (changed)
//		{
//			Services.notifications.localsChanged();
//		}
//		
//		Services.logger.logStream.println("Done synchronizing " + getCanonicalPath());
	}

	private static HashMap<String, File> key(ArrayList<File> subDirectories)
	{
		HashMap<String, File> returnValue = new HashMap<>();
		for (File file : subDirectories)
		{
			returnValue.put(file.getName(), file);
		}
		return returnValue;
	}
}
