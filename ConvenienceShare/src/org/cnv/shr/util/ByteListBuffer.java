package org.cnv.shr.util;

import java.util.LinkedList;

public final class ByteListBuffer extends AbstractByteWriter
{
	int length;
	private LinkedList<byte[]> bytesSoFar = new LinkedList<>();

	private static final int BUFFER_LENGTH = 256;
	private byte[] currentBuffer = new byte[BUFFER_LENGTH];
	private int offset;

	public ByteListBuffer append(byte[] bytes)
	{
		length += bytes.length;
		if (offset + bytes.length < currentBuffer.length)
		{
			System.arraycopy(bytes, 0, currentBuffer, offset, bytes.length);
			offset += bytes.length;
			return this;
		}
		
		for (int i = 0; i < bytes.length; i++)
		{
			checkEnd();
			currentBuffer[offset++] = bytes[i];
		}
		return this;
	}
	
	public ByteListBuffer append(byte i)
	{
		checkEnd();
		currentBuffer[offset++] = i;
		length++;
		return this;
	}

	public byte[] getBytes()
	{
		byte[] allBytes = new byte[length];
		int currentOffset = 0;
		for (byte[] cBytes : bytesSoFar)
		{
			System.arraycopy(cBytes, 0, allBytes, currentOffset, cBytes.length);
			currentOffset += cBytes.length;
		}
		System.arraycopy(currentBuffer, 0, allBytes, currentOffset, offset);
		
		bytesSoFar.clear();
		bytesSoFar.add(allBytes);
		offset = 0;

		return allBytes;
	}
	
	private void checkEnd()
	{
		if (offset < currentBuffer.length)
		{
			return;
		}
		bytesSoFar.add(currentBuffer);
		currentBuffer = new byte[BUFFER_LENGTH];
		offset = 0;
	}
	
	public long getLength()
	{
		return length;
	}
}
