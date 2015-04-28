package org.cnv.shr.util;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.cnv.shr.dmn.Settings;

/**
 * This class isn't really needed. We could write directly to the output stream,
 * and this would be better.
 * 
 * @author John
 * 
 */
public class ByteListBuffer
{
	int length;
	LinkedList<byte[]> currentBytes = new LinkedList<>();

	public ByteListBuffer append(byte[] bytes)
	{
		currentBytes.add(bytes);
		return this;
	}

	public ByteListBuffer append(byte i)
	{
		currentBytes.add(new byte[] { (byte) ((i >> 0) & 0xff), });
		return this;
	}

	public ByteListBuffer append(short i)
	{
		currentBytes.add(new byte[] { (byte) ((i >> 0) & 0xff), (byte) ((i >> 8) & 0xff), });
		return this;
	}

	public ByteListBuffer append(int i)
	{
		currentBytes.add(new byte[] { (byte) ((i >> 0) & 0xff), (byte) ((i >> 8) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 24) & 0xff), });
		return this;
	}

	public ByteListBuffer append(long i)
	{
		currentBytes.add(new byte[] { (byte) ((i >> 0L) & 0xff), (byte) ((i >> 8L) & 0xff), (byte) ((i >> 16L) & 0xff), (byte) ((i >> 24L) & 0xff), (byte) ((i >> 32L) & 0xff), (byte) ((i >> 40L) & 0xff), (byte) ((i >> 48L) & 0xff), (byte) ((i >> 56L) & 0xff), });
		return this;
	}

	public ByteListBuffer append(String str) throws UnsupportedEncodingException
	{
		byte[] bytes = str.getBytes(Settings.getInstance().getEncoding());
		append(bytes.length);
		append(bytes);
		return this;
	}

	public byte[] getBytes()
	{
		if (currentBytes.isEmpty())
		{
			return new byte[0];
		}
		if (currentBytes.size() == 1)
		{
			return currentBytes.get(0);
		}

		byte[] allBytes = new byte[length];
		int currentOffset = 0;
		for (byte[] cBytes : currentBytes)
		{
			for (int i = 0; i < cBytes.length; i++)
			{
				allBytes[currentOffset++] = cBytes[i];
			}
		}
		currentBytes.clear();
		currentBytes.add(allBytes);

		return allBytes;
	}

	/*
	 * 
	 * synchronized ByteBuffer append(byte i) { currentBytes.add(new byte[] {
	 * (byte)((i & 0x000000ff) >> 0), }); return this; } synchronized ByteBuffer
	 * append(short i) { currentBytes.add(new byte[] { (byte)((i & 0x0000ff00)
	 * >> 8), (byte)((i & 0x000000ff) >> 0), }); return this; } synchronized
	 * ByteBuffer append(int i) { currentBytes.add(new byte[] { (byte)((i &
	 * 0xff000000) >> 24), (byte)((i & 0x00ff0000) >> 16), (byte)((i &
	 * 0x0000ff00) >> 8), (byte)((i & 0x000000ff) >> 0), }); return this; }
	 * synchronized ByteBuffer append(long i) { currentBytes.add(new byte[] {
	 * (byte)((i & 0xff00000000000000L) >> 56L), (byte)((i &
	 * 0x00ff000000000000L) >> 48L), (byte)((i & 0x0000ff0000000000L) >> 40L),
	 * (byte)((i & 0x000000ff00000000L) >> 32L), (byte)((i &
	 * 0x00000000ff000000L) >> 24L), (byte)((i & 0x0000000000ff0000L) >> 16L),
	 * (byte)((i & 0x000000000000ff00L) >> 8L), (byte)((i & 0x00000000000000ffL)
	 * >> 0L), }); return this; }
	 */
}
