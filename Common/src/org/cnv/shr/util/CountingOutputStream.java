package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;

public final class CountingOutputStream extends OutputStream
{
	private OutputStream delegate;
	private long soFar;
	
	public CountingOutputStream(OutputStream newInputStream)
	{
		this.delegate = newInputStream;
	}

	public long getSoFar()
	{
		return soFar;
	}

	@Override
	public void write(int b) throws IOException
	{
		soFar++;
		delegate.write(b);
	}

	public void flush() throws IOException
	{
		delegate.flush();
	}
}