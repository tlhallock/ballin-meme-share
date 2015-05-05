
package org.cnv.shr.lcl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.util.FileOutsideOfRootException;

/**
 * I realize this would have been simpler with a method that recursively descended into subdirectories.
 * Instead, this keeps a stack inside the LocalDirectorySyncIterator of synchronization tasks to be performed.
 *
 */
public class LocalSynchronizer
{
	private static final Pair[] dummy = new Pair[0];
	
	LocalDirectorySyncIterator iterator;
	LocalDirectory local;
	
	public LocalSynchronizer(LocalDirectory dir)
	{
		iterator = new LocalDirectorySyncIterator(dir);
		this.local = dir;
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
		
//		Services.logger.logStream.println("Synchronizing " + task.current.getFullPath());
//		Services.logger.logStream.println("FS: " + task.files);
//		Services.logger.logStream.println("DB: " + task.dbPaths);
		
		LinkedList<Pair> subDirectories = new LinkedList<>();
		
		for (PathElement element : task.dbPaths)
		{
			accountedFor.add(element.getUnbrokenName());
			
			File fsCopy = files.get(element.getUnbrokenName());
			String fsPath = null;
			try
			{
				if (fsCopy != null)
				{
					fsPath = fsCopy.getCanonicalPath();
				}
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
				continue;
			}
			
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

				// remove directory from database
				DbPaths.pathDoesNotLieIn(element, local);
			}
			else
			{
				if (fsCopy.isFile())
				{
					if (dbVersion == null)
					{
						// add File
						addFile(element, fsCopy);
					}
					else
					{
						// update file
						try
						{
							dbVersion.refreshAndWriteToDb();
						}
						catch (SQLException e)
						{
							e.printStackTrace();
						}
					}
				}
				else if (fsCopy.isDirectory() && local.contains(fsPath))
				{
					// nothing to be done
					subDirectories.add(new Pair(fsCopy, element));
				}
			}
		}
		
		for (File f : files.values())
		{
			String name = f.getName();
			String fsPath = null;
			try
			{
				fsPath = f.getCanonicalPath();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			if (accountedFor.contains(f.getName()))
			{
				continue;
			}
			
			if (f.isDirectory())
			{
				name = name + "/";
			}
			
			// add path to database
			PathElement element = DbPaths.createPathElement(task.current, name);
			DbPaths.pathLiesIn(element, local);
			
			if (f.isFile())
			{
				// add file
				addFile(element, f);
			}
			else if (f.isDirectory() && local.contains(fsPath))
			{
				subDirectories.add(new Pair(f, element));
			}
		}
		
		
		task.synchronizedResults = subDirectories.toArray(dummy);
		
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

	private void addFile(PathElement element, File fsCopy)
	{
//		Services.logger.logStream.println("Found new file " + fsCopy);
		try
		{
			LocalFile lFile = new LocalFile(local, element);
			lFile.save();
		}
		catch (FileOutsideOfRootException ex)
		{
			Services.logger.logStream.println("Skipping symbolic link: "  + fsCopy);
		}
		catch (IOException ex)
		{
			Services.logger.logStream.println("Unable to get path of file: " + fsCopy);
			ex.printStackTrace();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	private static HashMap<String, File> key(ArrayList<File> subDirectories)
	{
		HashMap<String, File> returnValue = new HashMap<>();
		for (File file : subDirectories)
		{
			if (file.isFile())
			{
				returnValue.put(file.getName(), file);
			}
			else if (file.isDirectory())
			{
				returnValue.put(file.getName() + "/", file);
			}
		}
		return returnValue;
	}
}
