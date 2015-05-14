package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class EmptyMessage extends Message
{
	private int size;
	
	public EmptyMessage(int size)
	{
		this.size = size;
	}
	
	public EmptyMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	public static int TYPE = 30;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
		size = ByteReader.readInt(bytes);
		for (int i = 0; i < size; i++)
		{
			ByteReader.readByte(bytes);
		}
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(size);
		for (int i = 0; i < size; i++)
		{
			buffer.append((byte) 0);
		}
	}

	@Override
	public void perform(Communication connection) {}
	
	public String toString()
	{
		return "Filler of size " + size;
	}

	public boolean requiresAthentication()
	{
		return false;
	}
}
