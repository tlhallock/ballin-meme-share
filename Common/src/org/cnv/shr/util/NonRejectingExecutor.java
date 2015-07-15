package org.cnv.shr.util;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class NonRejectingExecutor
{
	private static final long TIMEOUT_MINUTES = 1;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private LinkedList<Runnable> moveQueue;
	
	private String name;
	
	private Worker[] workers;
	private boolean[] busy;
	
	private boolean shutDown;

	public NonRejectingExecutor(String name, int numThreads)
	{
		this.name = name;
		moveQueue = new LinkedList<>();
		busy = new boolean[numThreads];
		workers = new Worker[numThreads];
	}

	public synchronized void schedule(Runnable runnable, long delay)
	{
		Misc.timer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				if (shutDown) return;
				execute(runnable);
			}}, delay);
	}
	
	/* Called while lock is locked. */
	private void ensureAFree()
	{
		for (int i = 0; i < workers.length; i++)
		{
			if (workers[i] != null && !busy[i])
			{
				return;
			}
		}
		for (int i = 0; i < workers.length; i++)
		{
			if (workers[i] == null)
			{
				workers[i] = new Worker(i);
				workers[i].setName(name + "_worker_" + i);
				workers[i].start();
				busy[i] = false;
				return;
			}
		}
	}

	public void execute(Runnable runnable)
	{
		if (shutDown)
		{
			throw new RuntimeException("Already shutdown!");
		}
		
		lock.lock();
		try
		{
			ensureAFree();
			moveQueue.addFirst(runnable);
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void shutdown()
	{
		shutDown = true;
		
		lock.lock();
		try
		{
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
		
		for (Worker worker : workers)
		{
			worker.interrupt();
		}
	}
	
	public void finalize()
	{
		shutdown();
	}
	
	private final class Worker extends Thread
	{
		int index;
		
		public Worker(int index)
		{
			this.index = index;
		}
		
		public void run()
		{
			while (!shutDown)
			{
				Runnable next = null;
				lock.lock();
				try
				{
					busy[index] = false;
					
					if (moveQueue.isEmpty())
					{
						condition.await(TIMEOUT_MINUTES, TimeUnit.MINUTES);
					}
					if (moveQueue.isEmpty())
					{
						workers[index] = null;
						return;
					}
					next = moveQueue.removeLast();
					busy[index] = true;
				}
				catch (InterruptedException ex)
				{
					LogWrapper.getLogger().log(Level.INFO, "Interrupted", ex);
					workers[index] = null;
					break;
				}
				finally
				{
					lock.unlock();
				}

				try
				{
					next.run();
				}
				catch (Throwable t)
				{
					LogWrapper.getLogger().log(Level.INFO, "Caught throwable", t);
					
					if (t instanceof InterruptedException)
					{
						lock.lock();
						try
						{
							workers[index] = null;
							return;
						}
						finally
						{
							lock.unlock();
						}
					}
				}
			}
		}
	}
}
