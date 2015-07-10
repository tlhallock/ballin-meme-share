package org.cnv.shr.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.util.LogWrapper;

public class PathTreeModelRunnable extends WindowAdapter implements Runnable
{
	Lock lock = new ReentrantLock();
	Condition condition = lock.newCondition();
	
	RootSynchronizer synchronizer;
	private boolean quit;
	
	void quit()
	{
		lock.lock();
		try
		{
			quit = true;
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	void setSynchronizer(RootSynchronizer root)
	{
		lock.lock();
		try
		{
			synchronizer = root;
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void run()
	{
		while (!quit)
		{
			RootSynchronizer nextRoot;
			try
			{
				nextRoot = getNextRoot();
			}
			catch (InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted.", e);
				break;
			}
			if (quit || nextRoot == null)
			{
				break;
			}
			nextRoot.run();
		}
		
		LogWrapper.getLogger().info("Quiting path tree thread.");
	}

	private RootSynchronizer getNextRoot() throws InterruptedException
	{
		lock.lock();
		try
		{
			RootSynchronizer next;
			while ((next = synchronizer) == null && !quit)
			{
				condition.await(60, TimeUnit.SECONDS);
			}
			synchronizer = null;
			LogWrapper.getLogger().info("Found next root.");
			return next;
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		quit();
	}
}
