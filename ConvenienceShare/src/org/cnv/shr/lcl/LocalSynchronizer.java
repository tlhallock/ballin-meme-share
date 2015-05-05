
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
import org.cnv.shr.dmn.Notifications;
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
	
	int changeCount;
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
			try
			{
				testOnFs(files, accountedFor, subDirectories, element);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		for (File f : files.values())
		{
			try
			{
				testInDb(task, accountedFor, subDirectories, f);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		if (changeCount > 50)
		{
			changeCount = 0;
			Services.notifications.localsChanged();
		}
		
		
		task.synchronizedResults = subDirectories.toArray(dummy);
	}

	private void testInDb(SynchronizationTask task, HashSet<String> accountedFor, LinkedList<Pair> subDirectories, File f) throws IOException
	{
		String name = PathElement.sanitizeFilename(f);
		if (name == null 
				|| accountedFor.contains(f.getName())
				|| !local.contains(f.getCanonicalPath()))
		{
			return;
		}
		
		// add path to database
		PathElement element = DbPaths.createPathElement(task.current, name);
		DbPaths.pathLiesIn(element, local);
		
		if (f.isFile())
		{
			// add file
			addFile(element, f);
			changeCount++;
		}
		else if (f.isDirectory())
		{
			subDirectories.add(new Pair(f, element));
		}
	}

	private void testOnFs(HashMap<String, File> files, HashSet<String> accountedFor, LinkedList<Pair> subDirectories, PathElement element) throws IOException, SQLException
	{
		accountedFor.add(element.getUnbrokenName());

		File fsCopy = files.get(element.getUnbrokenName());
		LocalFile dbVersion = DbFiles.getFile(local, element);
		if (fsCopy == null)
		{
			if (dbVersion != null)
			{
				// delete stale file
				dbVersion.delete();
				changeCount++;
			}
			
			// remove directory from database
			DbPaths.pathDoesNotLieIn(element, local);
			
			return;
		}
		
		if (!local.contains(fsCopy.getCanonicalPath()))
		{
			return;
		}
		
		if (fsCopy.isDirectory())
		{
			// nothing to be done
			subDirectories.add(new Pair(fsCopy, element));
			return;
		}
		if (!fsCopy.isFile())
		{
			return;
		}
		if (dbVersion == null)
		{
			// add File
			addFile(element, fsCopy);
			changeCount++;
			return;
		}
		// update file
		if (dbVersion.refreshAndWriteToDb())
		{
			changeCount++;
		}
	}

	private void addFile(PathElement element, File fsCopy)
	{
		// Services.logger.logStream.println("Found new file " + fsCopy);
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
			String name = PathElement.sanitizeFilename(file);
			if (name != null)
			{
				returnValue.put(name, file);
			}
		}
		return returnValue;
	}
}
