package org.cnv.shr.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.FileSource.FileSourceIterator;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;

public class ExplorerSyncIterator extends SyncrhonizationTaskIterator
{
	private RootDirectory root;
	
	// Why did i have to write this queue myself?
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private boolean quit;
	
//	private LinkedBlockingDeque<SynchronizationTask> tasks = new LinkedBlockingDeque<>();
	private LinkedList<SynchronizationTask> tasks = new LinkedList<>();
	
	public ExplorerSyncIterator(final RootDirectory remoteDirectory) throws IOException
	{
		root = remoteDirectory;
	}

	@Override
	public SynchronizationTask next()
	{
		lock.lock();
		try
		{
			while (true)
			{
				if (quit) return null;
				
				if (!tasks.isEmpty())
				{
					return tasks.removeLast();
				}
				try
				{
					condition.await();
				}
				catch (final InterruptedException e)
				{
					Services.logger.print(e);
					return null;
				}
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public SynchronizationTask queueSyncTask(final FileSource file, final PathElement dbDir, final TaskListener listener) throws IOException
	{
		if (!file.stillExists())
		{
			return null;
		}

		lock.lock();
		try (FileSourceIterator grandChildren = file.listFiles();)
		{
			final SynchronizationTask synchronizationTask = new SynchronizationTask(dbDir, root, grandChildren);
			tasks.addFirst(synchronizationTask);
			synchronizationTask.addListener(listener);
			condition.signalAll();
			return synchronizationTask;
		}
		finally
		{
			lock.unlock();
		}
	}
	@Override
	public void close() throws IOException
	{
		lock.lock();
		try
		{
			quit = true;
			condition.signalAll();
			super.close();
		}
		finally
		{
			lock.unlock();
		}
	}
}
