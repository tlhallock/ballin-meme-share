package org.cnv.shr.util;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;

public final class ByteListBuffer
{
	private int length;
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
	
	public int getLength()
	{
		return length;
	}
	
	public ByteListBuffer append(byte i)
	{
		checkEnd();
		currentBuffer[offset++] = i;
		length++;
		return this;
	}

	public ByteListBuffer append(short i)
	{
		append((byte) ((i >>  8L) & 0xff));
		append((byte) ((i >>  0L) & 0xff)); 
		return this;
	}

	public ByteListBuffer append(int i)
	{
		append((byte) ((i >> 24L) & 0xff));
		append((byte) ((i >> 16L) & 0xff));
		append((byte) ((i >>  8L) & 0xff));
		append((byte) ((i >>  0L) & 0xff));
		return this;
	}

	public ByteListBuffer append(long i)
	{
		append((byte) ((i >> 56L) & 0xff));
		append((byte) ((i >> 48L) & 0xff));
		append((byte) ((i >> 40L) & 0xff));
		append((byte) ((i >> 32L) & 0xff));
		append((byte) ((i >> 24L) & 0xff));
		append((byte) ((i >> 16L) & 0xff));
		append((byte) ((i >>  8L) & 0xff));
		append((byte) ((i >>  0L) & 0xff));
		return this;
	}

	public ByteListBuffer append(String str)
	{
		byte[] bytes;
		try
		{
			bytes = str.getBytes(Settings.encoding);
		}
		catch (UnsupportedEncodingException e)
		{
			Services.logger.logStream.println("Encoding is not supported");
			e.printStackTrace(Services.logger.logStream);
			Main.quit();
			return this;
		}
		return append(bytes.length).append(bytes);
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
}
