package org.cnv.shr.ports;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.ports.IntervalPersistance.WrittenInterval;

class WindowInputStream extends IndexedInputStream
{
	private byte[] buffer;
	private IntervalPersistance persistance = new IntervalPersistance(0);
	private long totalRead;
	private long totalSent = -1;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	public WindowInputStream(int bufferSize)
	{
		buffer      = new byte[bufferSize];
		persistance = new IntervalPersistance(bufferSize);
	}
	
	public void write(byte[] other, int offset, int length, long startOffset) 
	{
		lock.lock();
		try
		{
			long prevWrite = totalRead - startOffset; 
			if (prevWrite > 0)
			{
				length 			-= prevWrite;
				offset 			+= prevWrite;
				startOffset += prevWrite;
			}
			int possibleWrite = (int) (totalRead + buffer.length - startOffset);
			if (length > possibleWrite)
			{
				length = possibleWrite;
			}
			
			while (length > 0)
			{
				int bufferStart = (int) (startOffset % buffer.length);
				int amountToWrite = length;
				if (amountToWrite > buffer.length - bufferStart)
				{
					amountToWrite = buffer.length - bufferStart;
				}
				persistance.add(bufferStart, bufferStart + amountToWrite);
				condition.signalAll();
				
				System.arraycopy(other, offset, buffer, bufferStart, amountToWrite);
				length      -= amountToWrite;
				offset      += amountToWrite;
				startOffset += amountToWrite;
			}
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public int read(byte[] other, int offset, int length) 
	{
		lock.lock();
		try
		{
			int totalReturned = 0;
			while (length > 0)
			{
				if (totalSent > 0 && length > totalSent - totalRead)
				{
					length = (int) (totalSent - totalRead);
					if (length <= 0)
					{
						return totalReturned > 0 ? totalReturned : -1;
					}
				}
				
				WrittenInterval nextBlock = persistance.getNextBlock();
				while (nextBlock == null)
				{
					if (totalReturned > 0)
					{
						return totalReturned;
					}
					condition.await(10, TimeUnit.MINUTES);
					nextBlock = persistance.getNextBlock();
				}

				int amountToRead = nextBlock.length();
				if (amountToRead > length)
				{
					amountToRead = length;
				}
				
				System.arraycopy(buffer, nextBlock.leftIndex, other, offset, amountToRead);
				offset        += amountToRead;
				totalReturned += amountToRead;
				length        -= amountToRead;
				totalRead 		+= amountToRead;
				persistance.remove(amountToRead);
			}
			return totalReturned;
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			return -1;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	private byte[] DUMMY = new byte[1];
	@Override
	public int read() throws IOException
	{
		int retVal = read(DUMMY, 0, 1);
		if (retVal < 0)
		{
			return -1;
		}
		return DUMMY[0] & 0xff;
	}

	@Override
	public int available()
	{
		return persistance.getNextBlock().length();
	}
	
	@Override
	public boolean markSupported()
	{
		return false;
	}
	
	@Override
	public void close() throws IOException
	{
		lock.lock();
		try
		{
			totalSent = totalRead;
		}
		finally
		{
			lock.unlock();
		}
	}

	public boolean isClosed()
	{
		return totalSent >= 0 && totalRead >= totalSent;
	}

	public long getAmountRead()
	{
		return totalRead;
	}
	
	public void setTotalSent(long totalSent)
	{
		this.totalSent = totalSent;
	}
}
