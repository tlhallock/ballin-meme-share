package org.cnv.shr.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.json.stream.JsonGenerator;

/**
 * The json parser continues to read after the current object.
 * When I change streams, this messes up the json.
 * To prevent this, I pause the stream with this class.
 * 
 * It has a special byte that signals the end of the current stream, so that we don't read past it.
 */
public class PausableInputStream extends InputStream
{
	static final int PAUSE_BYTE = 13;
	
	private InputStream delegate;
	private boolean paused;

	boolean rawMode;

	public PausableInputStream(InputStream input)
	{
		this.delegate = input;
	}
	public void setRawMode(boolean rawMode)
	{
		this.rawMode = rawMode;
	}
	public void startAgain(InputStream delegate)
	{
		paused = false;
		this.delegate = delegate;
	}

	@Override
	public int read() throws IOException
	{
		if (paused)
		{
			return -1;
		}
		int read = delegate.read();
		if (!rawMode && read == PAUSE_BYTE && delegate.read() != PAUSE_BYTE)
		{
			logFile.write("<paused here>"); logFile.flush();
			paused = true;
			return -1;
		}
		logFile.write(read); logFile.flush();
		return read;
	}

	public int read(byte b[]) throws IOException
	{
		return read(b, 0, b.length);
	}

	public int read(byte b[], int off, int len) throws IOException
	{
		if (rawMode)
		{
			return delegate.read(b, off, len);
		}
		if (b == null || b.length < 1)
		{
			throw new NullPointerException();
		}
		int read = read();
		if (read < 0)
		{
			return -1;
		}
		b[off] = (byte) read;
		return 1;
	}

	public long skip(long n) throws IOException
	{
		return delegate.skip(n);
	}

	public int available() throws IOException
	{
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

	
	
	
	
	
	
	
	
	
	

private BufferedWriter logFile; 
{ 
  Map<String, Object> properties = new HashMap<>(1);
  properties.put(JsonGenerator.PRETTY_PRINTING, true);
	try
	{
		String string = "log.in." + System.currentTimeMillis() + "." + Math.random() + ".txt";
		System.out.println("Logging to " + string);
		logFile = Files.newBufferedWriter(Paths.get(string));
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
}
