package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.json.stream.JsonGenerator;

public final class CountingInputStream extends InputStream
{
	private InputStream delegate;
	private long soFar;
	private boolean paused;
	
	
	
	
	
	
	
	
	
	
	
	

private OutputStream logFile; 
{ 
  Map<String, Object> properties = new HashMap<>(1);
  properties.put(JsonGenerator.PRETTY_PRINTING, true);
	try
	{
		logFile = Files.newOutputStream(Paths.get("log.in" + System.currentTimeMillis() + "." + Math.random() + ".txt"));
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public CountingInputStream(InputStream newInputStream)
	{
		this.delegate = newInputStream;
	}
	
	@Override
	public int read() throws IOException
	{
		if (paused)
		{
			return -1;
		}
		soFar++;
		int read = delegate.read();
		logFile.write(read);
		if (read == 13 && delegate.read() != 13)
		{
			paused = true;
			return -1;
		}
		return read;
	}
	
	public void startAgain()
	{
		paused = false;
	}
	
	public long getSoFar()
	{
		return soFar;
	}
	
	public long skip(long n) throws IOException
	{
		return delegate.skip(n);
	}

	public int available() throws IOException
	{
		return delegate.available();
	}

	public void close() throws IOException
	{
//		delegate.close();
	}

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
	
	public int read(byte b[], int off, int len) throws IOException
	{
		if (b == null || b.length < 1)
		{
			throw new NullPointerException();
		}
		int read =  read();
		if (read < 0)
		{
			return -1;
		}
		b[0] = (byte) read;
		return 1;
	}
}