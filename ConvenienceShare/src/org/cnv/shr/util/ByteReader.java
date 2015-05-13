package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;

import de.flexiprovider.common.math.FlexiBigInt;

public class ByteReader
{
	public static int readByte(InputStream in) throws IOException
	{
		return in.read() & 0xff;
	}

	public static int readShort(InputStream in) throws IOException
	{
		int i = 0;

		i |= (readByte(in)) << 8;
		i |= (readByte(in)) << 0;

		return i;
	}

	public static int readInt(InputStream in) throws IOException
	{
		long i = 0;

		i |= (readByte(in)) << 24L;
		i |= (readByte(in)) << 16L;
		i |= (readByte(in)) <<  8L;
		i |= (readByte(in)) <<  0L;

		return (int) i;
	}

	public static long readLong(InputStream in) throws IOException
	{
		long i = 0;

		i |= (readByte(in)) << 56L;
		i |= (readByte(in)) << 48L;
		i |= (readByte(in)) << 40L;
		i |= (readByte(in)) << 32L;
		i |= (readByte(in)) << 24L;
		i |= (readByte(in)) << 16L;
		i |= (readByte(in)) <<  8L;
		i |= (readByte(in)) <<  0L;

		return i;
	}

	public static String readString(InputStream in) throws IOException
	{
		return new String(readVarByteArray(in), Settings.encoding);
	}
	
	public static byte[] readVarByteArray(InputStream in) throws IOException
	{
		int size = (int) readInt(in);
		if (size > Services.settings.maxStringSize.get())
		{
			throw new IOException("Received string that is way too big!! Size=" + size);
		}

		byte[] returnValue = new byte[size];
		int readSoFar = 0;
		while (readSoFar < size && readSoFar >= 0)
		{
			readSoFar += in.read(returnValue, readSoFar, size - readSoFar);
		}
		if (readSoFar < size)
		{
			throw new IOException("Hit end of stream too early: " + readSoFar + " of " + size);
		}
		
		return returnValue;
	}
	
	public static PublicKey readPublicKey(InputStream bytes) throws IOException
	{
		byte[] readVarByteArray1 = readVarByteArray(bytes);
		byte[] readVarByteArray2 = readVarByteArray(bytes);
		if (readVarByteArray1.length == 0 || readVarByteArray2.length == 0)
		{
			return null;
		}
		FlexiBigInt publn = new FlexiBigInt(readVarByteArray1);
		FlexiBigInt puble = new FlexiBigInt(readVarByteArray2);
		return new de.flexiprovider.core.rsa.RSAPublicKey(publn, puble);
	}

	public static double readDouble(InputStream bytes) throws IOException
	{
		return Double.parseDouble(readString(bytes));
	}
}
