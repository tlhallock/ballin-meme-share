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
}