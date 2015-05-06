
package org.cnv.shr.lcl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TimerTask;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
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
public class LocalSynchronizer extends TimerTask
{
	private static final Pair[] dummy = new Pair[0];
	
	int changeCount;
	LocalDirectorySyncIterator iterator;
	LocalDirectory local;

	int filesAdded;
	long filesRefresh;
	long filesRemoved;
	long bytesAdded;
	String currentFile;
	
	public LocalSynchronizer(LocalDirectory dir)
	{
		iterator = new LocalDirectorySyncIterator(dir);
		this.local = dir;
	}
	
	public void synchronize()
	{
		SynchronizationTask task = null;
		while ((task = iterator.next()) != null)
		{
			synchronize(task);
		}
		if (changeCount > 0)
		{
			Services.notifications.localsChanged();
		}
	}
	
	private void synchronize(SynchronizationTask task)
	{
		HashMap<String, File> files = key(task.files);
		HashSet<String> accountedFor = new HashSet<>();
		
		currentFile = task.current.getFullPath();

		if (local.getCanonicalPath().getFullPath().equals("/home/thallock/Applications"))
		{
			Services.logger.logStream.println("Synchronizing " + task.current.getFullPath());
			Services.logger.logStream.println("FS: " + task.files);
			Services.logger.logStream.println("DB: " + task.dbPaths);
		}
		
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
		
		task.synchronizedResults = subDirectories.toArray(dummy);
		Thread.yield();
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
				remove(dbVersion);
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
			DbPaths.pathLiesIn(element, local);
			addFile(element, fsCopy);
			return;
		}
		update(dbVersion);
	}

	private void remove(LocalFile dbVersion) throws SQLException
	{
		// delete stale file
		dbVersion.delete();
		changeCount++;
		filesRemoved++;
	}

	private void update(LocalFile dbVersion) throws SQLException
	{
		// update file
		if (dbVersion.refreshAndWriteToDb())
		{
			changeCount++;
		}
		filesRefresh++;
	}

	private void addFile(PathElement element, File fsCopy)
	{
		// Services.logger.logStream.println("Found new file " + fsCopy);
		try
		{
			LocalFile lFile = new LocalFile(local, element);
			if (lFile.save())
			{
				changeCount++;
				filesAdded++;
				bytesAdded += fsCopy.length();
			}
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

	public static long DEBUG_REPEAT = 5000;
	private long lastDebug = System.currentTimeMillis();
	@Override
	public void run()
	{
		long now = System.currentTimeMillis();
		double seconds = (now - lastDebug) / 1000;

		synchronized (Services.logger.logStream)
		{
			Services.logger.logStream.println("-------------------------------------------------------");
			Services.logger.logStream.println("Synchronizing: " + local.getCanonicalPath().getFullPath());
			Services.logger.logStream.println("Current file: " + currentFile);
			Services.logger.logStream.println("File refresh rate: " + filesRefresh / seconds + "/s");
			Services.logger.logStream.println("File add rate: " + filesAdded / seconds + "/s");
			Services.logger.logStream.println("File remove rate: " + filesRemoved / seconds + "/s");
			Services.logger.logStream.println("-------------------------------------------------------");
		}
		changeCount = 0;
		
		local.setTotalFileSize(local.diskSpace() + bytesAdded);
		local.setTotalNumFiles(local.numFiles() + filesAdded - filesRemoved);
		Services.notifications.localChanged(local);
		
		lastDebug = now;
		filesRefresh = 0;
		filesAdded = 0;
		filesRemoved = 0;
		bytesAdded = 0;
	}
}
