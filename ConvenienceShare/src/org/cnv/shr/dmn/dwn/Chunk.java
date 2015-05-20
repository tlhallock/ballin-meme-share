package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.util.Scanner;

import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class Chunk
{
	private String checksum;
	private long begin;
	private long end;
	
	Chunk(String value)
	{
		try (Scanner s = new Scanner(value);)
		{
			begin = s.nextLong();
			end = s.nextLong();
			checksum = s.next();
		}
	}
	
	public Chunk(long begin, long end, String checksum)
	{
		this.begin = begin;
		this.end = end;
		this.checksum = checksum;
	}

	public Chunk(ByteReader input) throws IOException
	{
		read(input);
	}

	public void read(ByteReader input) throws IOException
	{
		begin =  input.readLong();
		end = input.readLong();
		checksum = input.readString();
	}

	public void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(getBegin());
		buffer.append(end);
		buffer.append(checksum == null ? "" : checksum);
	}
	
	@Override
	public String toString()
	{
		return getBegin() + " " + end + " " + checksum;
	}

	public void setChecksum(String checksum2)
	{
		this.checksum = checksum2;
	}

	public long getEnd()
	{
		return end;
	}

	public long getBegin()
	{
		return begin;
	}

	public String getChecksum()
	{
		return checksum;
	}

	public long getSize()
	{
		return end - getBegin();
	}
	
	public boolean equals(Chunk other)
	{
		return checksum.equals(other.checksum)
				&& getBegin() == other.getBegin() 
				&& end == other.end;
	}
}
