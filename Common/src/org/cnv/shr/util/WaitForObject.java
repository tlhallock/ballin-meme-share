package org.cnv.shr.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


// TODO: Refactor other uses of this, or use future...
public class WaitForObject<T>
{
	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private boolean found;
	private T object;
	private long timeout;
	
	public WaitForObject(int timeoutMillis)
	{
		this.timeout = System.currentTimeMillis() + timeoutMillis;
	}
	
	public void set(T t)
	{
		lock.lock();
		try
		{
			found = true;
			this.object = t;
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public T get() throws InterruptedException
	{
		lock.lock();
		try
		{
			if (object != null)
			{
				return object;
			}
			long now;
			while (!found && (now = System.currentTimeMillis()) < timeout)
			{
				condition.await(Math.max(1, timeout - now), TimeUnit.MILLISECONDS);
			}
			return object;
		}
		finally
		{
			lock.unlock();
		}
	}
}
