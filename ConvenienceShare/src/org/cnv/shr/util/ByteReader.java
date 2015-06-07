package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.stng.Settings;

import de.flexiprovider.common.math.FlexiBigInt;

public class ByteReader
{
	private InputStream in;
	
	public ByteReader(InputStream in, ConnectionStatistics stats)
	{
		this.in = in;
	}

	public ByteReader(InputStream is)
	{
		this.in = is;
	}
	
	public int readByte() throws IOException
	{
		int read = in.read();
		if (read < 0)
		{
			throw new IOException("Hit end of stream.");
		}
		return read & 0xff;
	}

	public int readShort() throws IOException
	{
		int i = 0;

		i |= readByte() << 8;
		i |= readByte() << 0;

		return i;
	}
	
	public long tryToReadInt() throws IOException
	{
		long i = 0;
		
		int firstByte = in.read();
		if (firstByte < 0)
		{
			return -1;
		}

		i |=      firstByte << 24L;
		i |= readByte() << 16L;
		i |= readByte() <<  8L;
		i |= readByte() <<  0L;

		return i;
	}
	
	public int readInt() throws IOException
	{
		long i = tryToReadInt();
		if (i < 0)
		{
			throw new IOException("Hit end of stream.");
		}
		return (int) i;
	}

	public long readLong() throws IOException
	{
		long i = 0;

		i |= readByte() << 56L;
		i |= readByte() << 48L;
		i |= readByte() << 40L;
		i |= readByte() << 32L;
		i |= readByte() << 24L;
		i |= readByte() << 16L;
		i |= readByte() <<  8L;
		i |= readByte() <<  0L;

		return i;
	}

	public String readString() throws IOException
	{
		return new String(readVarByteArray(), Settings.encoding);
	}
	
	public byte[] readVarByteArray() throws IOException
	{
		int size = readInt();
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
	
	public PublicKey readPublicKey() throws IOException
	{
		byte[] readVarByteArray1 = readVarByteArray();
		byte[] readVarByteArray2 = readVarByteArray();
		if (readVarByteArray1.length == 0 || readVarByteArray2.length == 0)
		{
			return null;
		}
		FlexiBigInt publn = new FlexiBigInt(readVarByteArray1);
		FlexiBigInt puble = new FlexiBigInt(readVarByteArray2);
		return new de.flexiprovider.core.rsa.RSAPublicKey(publn, puble);
	}
	
	public SharedFileId readSharedFileId() throws IOException
	{
		return new org.cnv.shr.dmn.dwn.SharedFileId(
				readString(),
				readString(),
				readString());
	}

	public double readDouble() throws IOException
	{
		return Double.parseDouble(readString());
	}

	public boolean readBoolean() throws IOException
	{
		return readByte() == 1 ? true : false;
	}

	public SharingState readPermission() throws IOException
	{
		return SharingState.get(readInt());
	}
}
