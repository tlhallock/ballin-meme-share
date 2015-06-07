package org.cnv.shr.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class OutputByteWriter extends AbstractByteWriter implements Closeable
{
	private OutputStream output;
	ConnectionStatistics stats;
	long length;

	public OutputByteWriter(OutputStream output)
	{
		this.output = output;
	}
	
	public OutputByteWriter(OutputStream output, ConnectionStatistics stats)
	{
		this.output = output;
		this.stats = stats;
	}

	@Override
	public AbstractByteWriter append(byte[] bytes)  throws IOException
	{
		output.write(bytes);
		length += bytes.length;
		return this;
	}

	@Override
	public AbstractByteWriter append(byte b) throws IOException
	{
		output.write(b);
		length++;
		return this;
	}

	@Override
	public long getLength()
	{
		return length;
	}

	@Override
	public void close() throws IOException
	{
		output.close();
	}
}
