
package org.cnv.shr.sync;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

/**
 * I realize this would have been simpler with a method that recursively descended into subdirectories.
 * Instead, this keeps a stack inside the LocalDirectorySyncIterator of synchronization tasks to be performed.
 *
 */
public abstract class RootSynchronizer implements Closeable
{
	private static final Pair[] dummy = new Pair[0];
	
	int changeCount;

	String currentFile;
	SyncrhonizationTaskIterator iterator;
	RootDirectory local;
	LinkedList<SynchronizationListener> listeners = new LinkedList<>();
	boolean quit;
	
	public RootSynchronizer(RootDirectory remoteDirectory, SyncrhonizationTaskIterator iterator) throws IOException
	{
		this.iterator = iterator;
		this.local = remoteDirectory;
	}
	
	public void quit()
	{
		quit = true;
	}
	
	public void addListener(SynchronizationListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(SynchronizationListener listener)
	{
		listeners.remove(listener);
	}
	
	public void synchronize()
	{
		SynchronizationTask task = null;
		while (!quit && (task = iterator.next()) != null)
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
		HashMap<String, FileSource> files = key(task.files);
		HashSet<String> accountedFor = new HashSet<>();
		
		currentFile = task.current.getFullPath();

		if (false)
		{
			Services.logger.logStream.println("Synchronizing " + task.current.getFullPath());
			Services.logger.logStream.println("FS: " + task.files);
			Services.logger.logStream.println("DB: " + task.dbPaths);
		}
		
		LinkedList<Pair<? extends FileSource>> subDirectories = new LinkedList<>();
		
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
		
		for (FileSource f : files.values())
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
		
		task.setResults(subDirectories.toArray(dummy));
		Thread.yield();
	}

	private void testInDb(SynchronizationTask task, HashSet<String> accountedFor, LinkedList<Pair<? extends FileSource>> subDirectories, FileSource f) throws IOException
	{
		String name = PathElement.sanitizeFilename(f);
		if (name == null 
				|| accountedFor.contains(f.getName())
				|| !local.pathIsSecure(f.getCanonicalPath()))
		{
			return;
		}
		
		// add path to database
		PathElement element = DbPaths.getPathElement(task.current, name);
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

	private void testOnFs(HashMap<String, FileSource> files, HashSet<String> accountedFor, LinkedList<Pair<? extends FileSource>> subDirectories, PathElement element) throws IOException, SQLException
	{
		accountedFor.add(element.getUnbrokenName());

		FileSource fsCopy = files.get(element.getUnbrokenName());
		SharedFile dbVersion = DbFiles.getFile(local, element);
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
		
		if (!local.pathIsSecure(fsCopy.getCanonicalPath()))
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

	private void remove(SharedFile dbVersion) throws SQLException
	{
		// delete stale file
		DbFiles.delete(dbVersion);
		changeCount++;

		for (SynchronizationListener listener : listeners)
		{
			listener.fileRemoved(dbVersion);
		}
	}

	protected abstract boolean updateFile(SharedFile file) throws SQLException;
	
	private void update(SharedFile dbVersion) throws SQLException
	{
		// update file
		if (!updateFile(dbVersion))
		{
			return;
		}
		changeCount++;
		for (SynchronizationListener listener : listeners)
		{
			listener.fileUpdated(dbVersion);
		}
	}

	private void addFile(PathElement element, FileSource fsCopy)
	{
		try
		{
			SharedFile lFile = fsCopy.create(local, element);
			if (lFile.save())
			{
				changeCount++;
				for (SynchronizationListener listener : listeners)
				{
					listener.fileAdded(lFile);
				}
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

	private static HashMap<String, FileSource> key(ArrayList<FileSource> files)
	{
		HashMap<String, FileSource> returnValue = new HashMap<>();
		for (FileSource file : files)
		{
			String name = PathElement.sanitizeFilename(file);
			if (name != null)
			{
				returnValue.put(name, file);
			}
		}
		return returnValue;
	}
	
	public void close() throws IOException
	{
		iterator.close();
	}

	public static interface SynchronizationListener
	{
		void fileAdded(SharedFile f);
		void fileRemoved(SharedFile f);
		void fileUpdated(SharedFile f);
	}
}
