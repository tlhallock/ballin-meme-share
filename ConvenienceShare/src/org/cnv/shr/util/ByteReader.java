package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;

public class ByteReader
{

	public static int readByte(InputStream in) throws IOException
	{
		return in.read() & 0xff;
	}

	public static int readShort(InputStream in) throws IOException
	{
		int i = 0;

		i |= (readByte(in)) << 0;
		i |= (readByte(in)) << 8;

		return i;
	}

	public static int readInt(InputStream in) throws IOException
	{
		long i = 0;

		i |= (readByte(in)) <<  0L;
		i |= (readByte(in)) <<  8L;
		i |= (readByte(in)) << 16L;
		i |= (readByte(in)) << 24L;

		return (int) i;
	}

	public static long readLong(InputStream in) throws IOException
	{
		long i = 0;

		i |= (readByte(in)) <<  0L;
		i |= (readByte(in)) <<  8L;
		i |= (readByte(in)) << 16L;
		i |= (readByte(in)) << 24L;
		i |= (readByte(in)) << 32L;
		i |= (readByte(in)) << 40L;
		i |= (readByte(in)) << 48L;
		i |= (readByte(in)) << 56L;

		return i;
	}

	public static String readString(InputStream in) throws IOException
	{
		int size = (int) readInt(in);
		if (size > Services.settings.maxStringSize.get())
		{
			throw new IOException("Received string that is way too big!! Size=" + size);
		}

		byte[] returnValue = new byte[size];
		int readSoFar = 0;
		while (readSoFar < size)
		{
			readSoFar += in.read(returnValue, readSoFar, size - readSoFar);
		}

		return new String(returnValue, Settings.encoding);
	}

	public static double readDouble(InputStream bytes) throws IOException
	{
		return Double.parseDouble(readString(bytes));
	}
}
