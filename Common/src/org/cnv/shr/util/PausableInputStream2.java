package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.cnv.shr.util.HardToCloseStreams.HardToCloseInputStream;


/**
 * The json parser continues to read after the current object.
 * When I change streams, this messes up the json.
 * To prevent this, I pause the stream with this class.
 * 
 * It has a special byte that signals the end of the current stream, so that we don't read past it.
 */
public class PausableInputStream2 extends HardToCloseInputStream
{
	public static final int PAUSE_BYTE = 13;
	
	private InputStream delegate;
	private boolean paused;

	boolean rawMode;
	
	
	int bufferBegin = 0;
	int bufferEnd = 0;
	byte[] buffer;
	byte[] singleReadBuffer = new byte[1];
	
	public PausableInputStream2(InputStream input)
	{
		this(input, Misc.BUFFER_SIZE);
	}
	public PausableInputStream2(InputStream input, int bufferSize)
	{
		this.delegate = input;
		buffer = new byte[bufferSize];
	}
	public void setRawMode(boolean rawMode)
	{
		this.rawMode = rawMode;
	}
	public void setDelegate(InputStream delegate)
	{
		this.delegate = delegate;
	}
	public void startAgain() throws IOException
	{
		while (!paused)
		{
			byte[] buffer = new byte[1024];
			int read = read(buffer, 0, buffer.length);
			if (read > 0)
			{
				LogWrapper.getLogger().info("Skipping " + read + " bytes: " + new String(Arrays.copyOf(buffer, read)));
			}
//			read(null, 0, Integer.MAX_VALUE);
		}
		paused = false;
	}

	@Override
	public int read() throws IOException
	{
		if (read(singleReadBuffer, 0, 1) < 0)
		{
			return -1;
		}
		return singleReadBuffer[0] & 0xff;
	}

	public int read(byte b[]) throws IOException
	{
		return read(b, 0, b.length);
	}
	
	
	
	private static final boolean DEBUG = true;
	private int delegateRead(byte buf[], int off, int len) throws IOException
	{
		int read = delegate.read(buf, off, len);
		
		if (DEBUG)
		{
			System.out.println("pausable byte returning " + new String(buf, off, len));
		}
		
		return read;
	}

	public int read(byte buf[], int off, int len) throws IOException
	{
		if (paused)
		{
			return -1;
		}
		if (rawMode)
		{
			return delegateRead(buf, off, len);
		}
		
		// if much?
		for (;;)
		{
			if (bufferBegin >= bufferEnd)
			{
				bufferBegin = 0;
				bufferEnd = delegateRead(buffer, 0, Math.min(buffer.length, len));
				if (bufferEnd < 0)
				{
					return -1;
				}
				continue;
			}
			if (buffer[bufferBegin] != PAUSE_BYTE)
			{
				int available = findNextRead();
				if (available <= 0)
				{
					throw new RuntimeException("WTH?");
				}
				if (available < len)
				{
					len = available;
				}
				if (buf!=null)
					System.arraycopy(buffer, bufferBegin, buf, off, len);
				bufferBegin += len;
				return len;
			}
			if (bufferEnd > bufferBegin + 1)
			{
				bufferBegin++;
				if (buffer[bufferBegin] == PAUSE_BYTE)
				{
					if (buf!=null)
						buf[off++] = PAUSE_BYTE;
					bufferBegin++;
					len--;
					int available = findNextRead();
					if (available <= 0)
					{
						return 1;
					}
					if (available < len)
					{
						len = available;
					}
					if (buf!=null)
						System.arraycopy(buffer, bufferBegin, buf, off, len);
					bufferBegin += len;
					return len + 1;
				}
				
				bufferBegin++;
				paused = true;
				return -1;
			}
			if (bufferEnd < buffer.length)
			{
				int remLen = buffer.length - bufferEnd;
				if (remLen > len)
				{
					remLen = len;
				}
				int read = delegateRead(buffer, bufferEnd, remLen);
				if (read < 0)
				{
					throw new IOException("Unescaped pause character!!");
				}
				bufferEnd += read;
				continue;
			}
			buffer[bufferBegin = 0] = PAUSE_BYTE;
			bufferEnd = 1 + delegateRead(buffer, 1, Math.min(buffer.length - 1, len));
			if (bufferEnd <= 0)
			{
				throw new IOException("Unescaped pause character!!");
			}
		}
	}

	public long skip(long n) throws IOException
	{
		return delegate.skip(n);
	}
	
	public int available() throws IOException
	{
		int returnValue = myAvailable();
//		System.out.println("Pausable available = " + returnValue);
		return returnValue;
	}
	public int myAvailable() throws IOException
	{
		if (bufferEnd - bufferBegin > 0)
		{
			if (bufferBegin + 1 > bufferEnd && buffer[bufferBegin] == PAUSE_BYTE && buffer[bufferBegin + 1] != PAUSE_BYTE)
			{
				return 0;
			}
//			System.out.println(Misc.format(Arrays.copyOfRange(buffer, bufferBegin, bufferEnd)));
			return bufferEnd - bufferBegin;
		}
		
		return delegate.available();
	}

	public void close() throws IOException {}

	public synchronized void mark(int readlimit)
	{
		delegate.mark(readlimit);
	}

	public synchronized void reset() throws IOException
	{
		delegate.reset();
	}

	public boolean markSupported()
	{
		return delegate.markSupported();
	}
	
	private int findNextRead()
	{
		int len = bufferEnd - bufferBegin;
		for (int next = 0; next < len; next++)
		{
			if (buffer[bufferBegin + next] == PAUSE_BYTE)
			{
				return next;
			}
		}
		return len;
	}
	
	@Override
	public void actuallyClose() throws IOException
	{
		delegate.close();
	}
}
