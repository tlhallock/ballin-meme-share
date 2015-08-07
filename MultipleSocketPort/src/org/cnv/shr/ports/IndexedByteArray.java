package org.cnv.shr.ports;

import java.io.IOException;


class IndexedByteArray
{
	private byte[] input;
	private int offset;
	private int maximum;
	
	public IndexedByteArray(byte[] buf)
	{
		this.input = buf;
	}
	

	public void reset()
	{
		reset(MetaMsg.MESSAGE_START, MetaMsg.MAXIMUM_MESSAGE_SIZE);
	}
	
	public void reset(int start, int newMaximum)
	{
		offset = start;
		maximum = newMaximum;
	}

	public int readInt() throws IOException
	{
		int returnValue = 0;
		
		if (offset + 4 > maximum)
		{
			throw new IOException("Read past end of packet.");
		}

		returnValue |= (input[offset++] & 0xff) <<  0;
		returnValue |= (input[offset++] & 0xff) <<  8;
		returnValue |= (input[offset++] & 0xff) << 16;
		returnValue |= (input[offset++] & 0xff) << 24;
		
		return returnValue;
	}

	public void writeInt(int value) throws IOException
	{
		if (offset + 4 > maximum)
		{
			throw new IOException("Read past end of packet.");
		}
		
		input[offset++] = (byte)((value >>  0) & 0xff);
		input[offset++] = (byte)((value >>  8) & 0xff);
		input[offset++] = (byte)((value >> 16) & 0xff);
		input[offset++] = (byte)((value >> 24) & 0xff);
	}

	public long readLong() throws IOException
	{
		long returnValue = 0;
		
		if (offset + 8 > maximum)
		{
			throw new IOException("Read past end of packet.");
		}

		returnValue |= (input[offset++] & 0xffL) <<  0;
		returnValue |= (input[offset++] & 0xffL) <<  8;
		returnValue |= (input[offset++] & 0xffL) << 16;
		returnValue |= (input[offset++] & 0xffL) << 24;
		returnValue |= (input[offset++] & 0xffL) << 32;
		returnValue |= (input[offset++] & 0xffL) << 40;
		returnValue |= (input[offset++] & 0xffL) << 48;
		returnValue |= (input[offset++] & 0xffL) << 56;
		
		return returnValue;
	}

	public void writeLong(long value) throws IOException
	{
		if (offset + 8 > maximum)
		{
			throw new IOException("Read past end of packet.");
		}
		
		input[offset++] = (byte)((value >>  0) & 0xffL);
		input[offset++] = (byte)((value >>  8) & 0xffL);
		input[offset++] = (byte)((value >> 16) & 0xffL);
		input[offset++] = (byte)((value >> 24) & 0xffL);
		input[offset++] = (byte)((value >> 32) & 0xffL);
		input[offset++] = (byte)((value >> 40) & 0xffL);
		input[offset++] = (byte)((value >> 48) & 0xffL);
		input[offset++] = (byte)((value >> 56) & 0xffL);
	}

	public int getOffset()
	{
		return offset;
	}
	
	public byte[] getBytes()
	{
		return input;
	}
}
