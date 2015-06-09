
package org.cnv.shr.sync;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;

/**
 * I realize this would have been simpler with a method that recursively descended into subdirectories.
 * Instead, this keeps a stack inside the LocalDirectorySyncIterator of synchronization tasks to be performed.
 *
 */
public abstract class RootSynchronizer implements Runnable
{
	private static final Pair[] dummy = new Pair[0];
	
	private int changeCount;

	protected RootDirectory local;
	
	private String currentFile;
	private SyncrhonizationTaskIterator iterator;
	private LinkedList<SynchronizationListener> listeners = new LinkedList<>();
	private boolean quit;
	
	public RootSynchronizer(final RootDirectory remoteDirectory, final SyncrhonizationTaskIterator iterator)
	{
		this.iterator = iterator;
		this.local = remoteDirectory;
	}
	
	public void quit()
	{
		quit = true;
		try
		{
			iterator.close();
		}
		catch (final IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close iterator", e);
		}
	}
	
	public void addListener(final SynchronizationListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(final SynchronizationListener listener)
	{
		listeners.remove(listener);
	}
	
	@Override
	public void run()
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
		try
		{
			iterator.close();
		}
		catch (final IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close iterator", e);
		}
	}
	
	private void synchronize(final SynchronizationTask task)
	{
		DbPaths.pathLiesIn(DbPaths.ROOT, local);
		
		final HashMap<String, FileSource> files = key(task.files);
		final HashSet<String> accountedFor = new HashSet<>();
		
		currentFile = task.current.getFullPath();
		for (final SynchronizationListener listener : listeners)
		{
			listener.beganDirectory(currentFile);
		}

		if (LogWrapper.getLogger().isLoggable(Level.FINE))
		{
			LogWrapper.getLogger().fine("Synchronizing " + task.current.getFullPath());
			LogWrapper.getLogger().fine("FS: " + task.files);
			LogWrapper.getLogger().fine("DB: " + task.dbPaths);
		}
		
		final LinkedList<Pair> subDirectories = new LinkedList<>();
		
		for (final PathElement element : task.dbPaths)
		{
			try
			{
				testOnFs(files, accountedFor, subDirectories, element);
			}
			catch (final Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to test on fs: " + element, e);
			}
		}
		
		for (final FileSource f : files.values())
		{
			try
			{
				testInDb(task, accountedFor, subDirectories, f);
			}
			catch (final IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to test in db: " + f, e);
			}
		}
		
		task.setResults(subDirectories.toArray(dummy));
		Thread.yield();
	}

	private void testInDb(final SynchronizationTask task, 
			final HashSet<String> accountedFor, 
			final LinkedList<Pair> subDirectories, 
			final FileSource f) throws IOException
	{
		final String name = PathElement.sanitizeFilename(f);
		if (name == null 
				|| accountedFor.contains(name))
		{
			return;
		}
		
		// add path to database
		final PathElement element = DbPaths.getPathElement(task.current, name);
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

	private void testOnFs(final HashMap<String, FileSource> files, 
			final HashSet<String> accountedFor, 
			final LinkedList<Pair> subDirectories, 
			final PathElement element) throws IOException, SQLException
	{
		accountedFor.add(element.getUnbrokenName());

		final FileSource fsCopy = files.get(element.getUnbrokenName());
		final SharedFile dbVersion = DbFiles.getFile(local, element);
		if (fsCopy == null)
		{
			if (dbVersion != null)
			{
				remove(dbVersion);
			}
			
			// remove directory from database
			DbPaths.pathDoesNotLieIn(element.getBrokenBegin(), local);
			
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

	private void remove(final SharedFile dbVersion) throws SQLException
	{
		// delete stale file
		DbFiles.delete(dbVersion);
		changeCount++;

		for (final SynchronizationListener listener : listeners)
		{
			listener.fileRemoved(dbVersion);
		}
	}

	protected abstract boolean updateFile(SharedFile file) throws SQLException, IOException;
	
	private void update(final SharedFile dbVersion) throws SQLException, IOException
	{
		// update file
		if (!updateFile(dbVersion))
		{
			return;
		}
		changeCount++;
		for (final SynchronizationListener listener : listeners)
		{
			listener.fileUpdated(dbVersion);
		}
	}

	private void addFile(final PathElement element, final FileSource fsCopy)
	{
		try
		{
			final SharedFile lFile = fsCopy.create(local, element);
			if (!lFile.tryToSave())
			{
				return;
			}
			changeCount++;
			for (final SynchronizationListener listener : listeners)
			{
				listener.fileAdded(lFile);
			}
			Services.notifications.fileAdded(lFile);
		}
		catch (final FileOutsideOfRootException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "File was outside of root "  + fsCopy, ex);
		}
		catch (final IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get path of file: " + fsCopy, ex);
		}
	}

	private static HashMap<String, FileSource> key(final ArrayList<FileSource> files)
	{
		final HashMap<String, FileSource> returnValue = new HashMap<>();
		for (final FileSource file : files)
		{
			final String name = PathElement.sanitizeFilename(file);
			if (name != null)
			{
				returnValue.put(name, file);
			}
		}
		return returnValue;
	}
	
	public static interface SynchronizationListener
	{
		void beganDirectory(String str);
		void fileAdded(SharedFile f);
		void fileRemoved(SharedFile f);
		void fileUpdated(SharedFile f);
	}
}
