package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;

public final class CountingInputStream extends InputStream
{
	private InputStream delegate;
	private long soFar;
	
	public CountingInputStream(InputStream newInputStream)
	{
		this.delegate = newInputStream;
	}
	
	@Override
	public int read() throws IOException
	{
		soFar++;
		return delegate.read();
	}
	
	public long getSoFar()
	{
		return soFar;
	}
	
	
	// OMG
	public int read(byte b[]) throws IOException
	{
		return delegate.read(b);
	}

	public int read(byte b[], int off, int len) throws IOException
	{
		return delegate.read(b, off, len);
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
		delegate.close();
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
}