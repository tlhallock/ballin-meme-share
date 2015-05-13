package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;

public class OutputByteWriter extends AbstractByteWriter
{
	private OutputStream output;
	long length;
	
	public OutputByteWriter(OutputStream output)
	{
		this.output = output;
	}

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
}
