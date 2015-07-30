package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EverythingSoFarInputStream extends InputStream implements Runnable
{
	private InputStream delegate;
	
	private byte[] buffer;
	private int bufferStart;
	private int bufferEnd;
	
	boolean closed;
	
	private transient Lock lock = new ReentrantLock();
	private transient Condition condition = lock.newCondition();

	public EverythingSoFarInputStream(InputStream delegate, int bufferSize)
	{
		this.delegate = delegate;
		this.buffer = new byte[bufferSize];
	}
	
	@Override
	public int available() throws IOException
	{
		return bufferEnd - bufferStart + delegate.available();
	}
	
	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				int startRead;
				int readLen;

				lock.lock();
				try
				{
					if (bufferStart == bufferEnd)
					{
						bufferStart = 0;
						bufferEnd = 0;
					}
					startRead = bufferEnd;
					readLen = buffer.length - startRead;
					if (readLen == 0)
					{
						condition.await();
						continue;
					}
				}
				finally
				{
					lock.unlock();
				}

				int read = delegate.read(buffer, startRead, readLen);

				lock.lock();
				try
				{
					if (read > 0)
						bufferEnd += read;
					else
						closed = true;
					condition.signalAll();
				}
				finally
				{
					lock.unlock();
				}
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			closed = true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			closed = true;
		}
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException
	{
		if (closed) return -1;
		
		lock.lock();
		try
		{
			while (bufferStart == bufferEnd)
			{
				if (closed) return -1;
				condition.await();
			}

			if (closed) return -1;
			if (len > bufferEnd - bufferStart)
			{
				len = bufferEnd - bufferStart;
			}
			
			System.arraycopy(buffer, bufferStart, buf, off, len);
			bufferStart += len;
			
			condition.signalAll();
			return len;
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			closed = true;
			return -1;
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public int read() throws IOException
	{
		byte[] tmpArray = new byte[1];
		int retVal = read(tmpArray, 0, 1);
		if (retVal < 0)
		{
			return retVal;
		}
		return tmpArray[0];
	}
}
