package org.cnv.shr.ports;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Window
{
	private static final int BUFFER_SIZE = 8192;
	
	// readOffset == writeOffset     => blocked read
	// readOffset == writeOffset + 1 => blocked write
	
	private int readOffset;
	private int writeOffset;
	private long totalWritten;
	private byte[] buffer;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	public Window(int bufferSize)
	{
		buffer = new byte[bufferSize];
	}
	
	public void write(
			byte[] values,
			int offset,
			int length,
			long startingOffset)
	{
		lock.lock();
		try
		{
			while (length > 0)
			{
				int bufferIndexBegin = writeOffset;
				int bufferIndexEnd   = bufferIndexBegin + length;
				if (bufferIndexEnd >= buffer.length)
				{
					if (readOffset == 0)
					{
						bufferIndexEnd = buffer.length - 1;
					}
					else
					{
						bufferIndexEnd = buffer.length;
					}
				}
				if (bufferIndexBegin < readOffset && bufferIndexEnd + 1 >= readOffset)
				{
						bufferIndexEnd = readOffset - 1;
				}
				int writeLength = bufferIndexEnd - bufferIndexBegin;
				if (writeLength <= 0)
				{
					System.out.println("Buffer full, waiting.");
					condition.await(10, TimeUnit.MINUTES);
					continue;
				}
				
				System.out.println("Writing from " + bufferIndexBegin + " to " + bufferIndexEnd);
				
				length -= writeLength;
				writeOffset = (writeOffset + writeLength) % buffer.length;
				totalWritten += length;
				
				condition.signalAll();
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
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
			int totalWritten = 0;
			while (length > 0)
			{
				int bufferIndexBegin = readOffset;
				int bufferIndexEnd = bufferIndexBegin + length;
				if (bufferIndexEnd >= buffer.length)
				{
					bufferIndexEnd = buffer.length;
				}
				if (bufferIndexBegin <= writeOffset && bufferIndexEnd > writeOffset)
				{
					bufferIndexEnd = writeOffset;
				}
				int readLength = bufferIndexEnd - bufferIndexBegin;
				if (readLength <= 0)
				{
					if (totalWritten > 0)
					{
						return totalWritten;
					}
					System.out.println("Buffer empty, waiting.");
					condition.await(10, TimeUnit.MINUTES);
					continue;
				}

				System.out.println("Read from " + bufferIndexBegin + " to " + bufferIndexEnd);

				length -= readLength;
				totalWritten += readLength;
				readOffset = (readOffset + readLength) % buffer.length;
				condition.signalAll();
			}

			return totalWritten;
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
}
