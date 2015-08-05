package org.cnv.shr.ports;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WindowOutputStream extends OutputStream
{
	private static final int BUFFER_SIZE = 1024;
	
	private long totalRead;
	private long totalWritten;
	
	private byte[] buffer;
	
	private boolean closed;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	public WindowOutputStream(int bufferSize)
	{
		buffer = new byte[bufferSize];
	}
	
	
	public static final class Interval { long start; long stop; public Interval(long b, long e) { this.start = b; this.stop = e; } };
	public Interval getWindow()
	{
		return new Interval(totalRead, totalWritten);
	}
	
	/**
	 * Write the bytes in values from offset to offset + length.
	 * This method blocks until all bytes can be written.
	 */
	public void write(
			byte[] values,
			int offset,
			int length)
	{
		lock.lock(); 
		try
		{
			while (length > 0)
			{
				int amountToWrite = (int) (buffer.length - (totalWritten - totalRead)); 
				if (amountToWrite <= 0)
				{
					condition.await(10, TimeUnit.MINUTES);
					continue;
				}
				
				int bufferIndexBegin = (int) (totalWritten % buffer.length);
				int remBuffer = buffer.length - bufferIndexBegin;
				if (amountToWrite > remBuffer) 
				{
					amountToWrite = remBuffer;
				}
				if (amountToWrite > length)
				{
					amountToWrite = length;
				}
				System.arraycopy(values, offset, buffer, bufferIndexBegin, amountToWrite);
				
				length       -= amountToWrite;
				offset       += amountToWrite;
				totalWritten += amountToWrite;
				
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
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[totalRead = " + totalRead + "]");
		builder.append("[totalWritten = " + totalWritten + "]");
		builder.append("[bytes=");
		for (byte b : buffer)
		{
			builder.append(String.format("%02X", b & 0xff));
		}
		builder.append("]");
		
		return builder.toString();
	}
	
	/**
	 *  Mark all bytes before newTotalRead as already read, so that more bytes can be written.
	 */
	public void setRead(long newTotalRead) throws IOException
	{
		lock.lock();
		try
		{
			if (newTotalRead < totalRead)
			{
				throw new IOException("Unable to go backwards from " + totalRead + " to " + newTotalRead);
			}
			if (newTotalRead > totalWritten)
			{
				throw new IOException("Unable to read past what is written!");
			}
			totalRead = newTotalRead;
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	/**
	 *
	 * Read bytes into other that have already been written.
	 * All bytes in the range from startOffset to startOffset + length must still be in window.
	 */
	public void read(byte[] other, int offset, int length, long startOffset) throws IOException
	{
		lock.lock();
		try
		{
			if (startOffset < totalRead || startOffset + length > totalWritten)
			{
				throw new IOException("Bytes no longer in window!");
			}
			
			while (length > 0)
			{
				int bufferIndexBegin = (int) (startOffset % buffer.length);
				int bufferIndexEnd = bufferIndexBegin + length;
				if (bufferIndexEnd >= buffer.length)
				{
					bufferIndexEnd = buffer.length;
				}
				int readLength = bufferIndexEnd - bufferIndexBegin;
				
				System.arraycopy(buffer, bufferIndexBegin, other, offset, readLength);
				
				length      -= readLength;
				offset      += readLength;
				startOffset += readLength;
			}
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private byte[] DUMMY = new byte[1];
	@Override
	public void write(int b) throws IOException
	{
		DUMMY[0] = (byte) b;
		write(DUMMY, 0, 1);
	}

	public boolean isClosed()
	{
		return closed;
	}
	
	@Override
	public void close() throws IOException
	{
		closed = true;
	}
}
