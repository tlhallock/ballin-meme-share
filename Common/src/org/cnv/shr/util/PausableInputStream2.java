package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The json parser continues to read after the current object.
 * When I change streams, this messes up the json.
 * To prevent this, I pause the stream with this class.
 * 
 * It has a special byte that signals the end of the current stream, so that we don't read past it.
 */
public class PausableInputStream2 extends InputStream
{
	public static final int PAUSE_BYTE = 13;
	
	private InputStream delegate;
	private boolean paused;

	boolean rawMode;
	
	
	int bufferBegin = 0;
	int bufferEnd = 0;
	byte[] buffer;
	byte[] singleReadBuffer = new byte[1];
	
	OutputStream raw;

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
		if (rawMode)
		{
			try
			{
				raw = Files.newOutputStream(Paths.get("log.Raw.in" + Math.random()));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				raw.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void setDelegate(InputStream delegate)
	{
		this.delegate = delegate;
	}
	public void startAgain()
	{
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

	public int read(byte b[], int off, int len) throws IOException
	{
		if (paused)
		{
			return -1;
		}
		if (rawMode)
		{
			int nread = delegate.read(b, off, len);
//			logFile.write(b, off, nread); logFile.flush();
			return nread;
		}

		// if much?
		for (;;)
		{
			if (bufferBegin >= bufferEnd)
			{
				bufferBegin = 0;
				bufferEnd = delegate.read(buffer, 0, Math.min(buffer.length, len));
				if (bufferEnd < 0)
				{
					return -1;
				}
//				logFile.write(bufferEnd); logFile.flush();
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
				System.arraycopy(buffer, bufferBegin, b, off, len);
				bufferBegin += len;
				return len;
			}
			if (bufferEnd > bufferBegin + 1)
			{
				bufferBegin++;
				if (buffer[bufferBegin] == PAUSE_BYTE)
				{
					b[off++] = PAUSE_BYTE;
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
					System.arraycopy(buffer, bufferBegin, b, off, len);
					bufferBegin += len;
					return len + 1;
				}
				
				bufferBegin++;
				paused = true;
//				logFile.write("<paused here>".getBytes());
//				logFile.flush();
				return -1;
			}
			if (bufferEnd < buffer.length)
			{
				int remLen = buffer.length - bufferEnd;
				if (remLen > len)
				{
					remLen = len;
				}
				int read = delegate.read(buffer, bufferEnd, remLen);
				if (read < 0)
				{
					throw new IOException("Unescaped pause character!!");
				}
//				logFile.write(read); logFile.flush();
				bufferEnd += read;
				continue;
			}
			// facts:
			// buffer[bufferBegin] == PAUSE_BYTE
			// bufferBegin + 1 == bufferEnd
			// bufferEnd == buffer.length
			buffer[bufferBegin = 0] = PAUSE_BYTE;
			bufferEnd = 1 + delegate.read(buffer, 1, Math.min(buffer.length - 1, len));
			if (bufferEnd < 0)
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
		if (bufferEnd - bufferBegin > 0)
		{
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

//	private OutputStream logFile;
//	{
//		Map<String, Object> properties = new HashMap<>(1);
//		properties.put(JsonGenerator.PRETTY_PRINTING, true);
//		try
//		{
//			String string = "log.in." + System.currentTimeMillis() + "." + Math.random() + ".txt";
//			System.out.println("Logging to " + string);
//			logFile = Files.newOutputStream(Paths.get(string));
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//	}
}
