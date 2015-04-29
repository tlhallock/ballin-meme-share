package org.cnv.shr.util;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.Settings;

public class ByteListBuffer
{
	private int length;
	private LinkedList<byte[]> currentBytes = new LinkedList<>();

	public ByteListBuffer append(byte[] bytes)
	{
		currentBytes.add(bytes);
		return this;
	}

	public ByteListBuffer append(byte i)
	{
		currentBytes.add(new byte[] {
				(byte) ((i >> 0) & 0xff),
		});
		return this;
	}

	public ByteListBuffer append(short i)
	{
		currentBytes.add(new byte[] { 
				(byte) ((i >> 0) & 0xff), 
				(byte) ((i >> 8) & 0xff), 
		});
		return this;
	}

	public ByteListBuffer append(int i)
	{
		currentBytes.add(new byte[] { 
				(byte) ((i >>  0) & 0xff), 
				(byte) ((i >>  8) & 0xff), 
				(byte) ((i >> 16) & 0xff), 
				(byte) ((i >> 24) & 0xff),
		});
		return this;
	}

	public ByteListBuffer append(long i)
	{
		currentBytes.add(new byte[] { 
				(byte) ((i >>  0L) & 0xff), 
				(byte) ((i >>  8L) & 0xff), 
				(byte) ((i >> 16L) & 0xff), 
				(byte) ((i >> 24L) & 0xff), 
				(byte) ((i >> 32L) & 0xff), 
				(byte) ((i >> 40L) & 0xff), 
				(byte) ((i >> 48L) & 0xff), 
				(byte) ((i >> 56L) & 0xff), 
		});
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
}
