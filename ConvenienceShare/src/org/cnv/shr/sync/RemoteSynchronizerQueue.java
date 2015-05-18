package org.cnv.shr.sync;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.msg.PathList;
import org.cnv.shr.msg.ListPath;

public class RemoteSynchronizerQueue implements Closeable
{
	private static final int MAXIMUM_ERROR_COUNT = 100;
	private int errorCount = 0;
	
	private long MAX_TIME_TO_WAIT = 2 * 60 * 1000;
	private long lastCommunication;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private HashMap<String, PathList> directories = new HashMap<>();
	Communication communication;
	RemoteDirectory root;
	private HashSet<String> queue = new HashSet<>();
	
	public RemoteSynchronizerQueue(final Communication c, final RemoteDirectory root)
	{
		communication = c;
		this.root = root;
	}
	
	private PathList waitForDirectory(final String path)
	{
		for (;;)
		{
			final PathList directoryList = directories.get(path);
			if (directoryList != null)
			{
				directories.remove(path);
				return directoryList;
			}
			if (errorCount > MAXIMUM_ERROR_COUNT)
			{
				return null;
			}
			try
			{
				condition.await(20, TimeUnit.SECONDS);
			}
			catch (final InterruptedException e)
			{
				// TODO: Make sure this is handled well...
				e.printStackTrace();
				communication.close();
				return null;
			}
			if (communication.isClosed() || System.currentTimeMillis() - lastCommunication > MAX_TIME_TO_WAIT)
			{
				errorCount++;
				return null;
			}
		}
	}
	
	PathList getDirectoryList(final PathElement pathElement)
	{
		final String path = pathElement.getFullPath();
		lock.lock();
		try
		{
			if (communication.isClosed())
			{
				return null;
			}
			ensureQueued(pathElement);
			return waitForDirectory(path);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void receiveList(final PathList list)
	{
		lock.lock();
		try
		{
			directories.put(list.getCurrentPath(), list);
			queue.remove(list.getCurrentPath());
			lastCommunication = System.currentTimeMillis();
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private void ensureQueued(final PathElement path)
	{
		if (queue.contains(path.getFullPath())
				|| directories.containsKey(path.getFullPath()))
		{
			return;
		}
		if (communication.isClosed())
		{
			return;
		}
		lastCommunication = System.currentTimeMillis();
		communication.send(new ListPath(root, path));
		queue.add(path.getFullPath());
	}
	
	public void queueDirectoryList(final PathElement path)
	{
		lock.lock();
		try
		{
			ensureQueued(path);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	@Override
	public void close()
	{
		lock.lock();
		try
		{
			Services.syncs.done(this);
			condition.signalAll();
			communication.finish();
		}
		finally
		{
			lock.unlock();
		}
	}
}