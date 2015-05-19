package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;

import org.cnv.shr.cnctn.ConnectionStatistics;

public class OutputByteWriter extends AbstractByteWriter
{
	private OutputStream output;
	ConnectionStatistics stats;
	long length;

	public OutputByteWriter(OutputStream output)
	{
		this.output = output;
		this.stats = new ConnectionStatistics();
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
		stats.bytesSent(bytes.length);
		length += bytes.length;
		return this;
	}

	@Override
	public AbstractByteWriter append(byte b) throws IOException
	{
		output.write(b);
		length++;
		stats.bytesSent(1);
		return this;
	}

	@Override
	public long getLength()
	{
		return length;
	}
}
