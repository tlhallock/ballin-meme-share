package org.cnv.shr.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.stng.Settings;

/**
 * I probably don't need the ByteListBuffer anymore, so this class could be collapsed with the ByteWriter.
 */
public abstract class AbstractByteWriter
{
	public abstract AbstractByteWriter append(byte b) throws IOException;
	public abstract long getLength();

	public AbstractByteWriter append(byte[] bytes)  throws IOException
	{
		for (byte b : bytes)
		{
			append(b);
		}
		return this;
	}

	public AbstractByteWriter append(short i) throws IOException
	{
		append((byte) ((i >>  8L) & 0xff));
		append((byte) ((i >>  0L) & 0xff)); 
		return this;
	}

	public AbstractByteWriter append(int i) throws IOException
	{
		append((byte) ((i >> 24L) & 0xff));
		append((byte) ((i >> 16L) & 0xff));
		append((byte) ((i >>  8L) & 0xff));
		append((byte) ((i >>  0L) & 0xff));
		return this;
	}

	public AbstractByteWriter append(long i) throws IOException
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

	public AbstractByteWriter append(String str) throws IOException
	{
		try
		{
			return appendVarByteArray(str.getBytes(Settings.encoding));
		}
		catch (UnsupportedEncodingException e)
		{
			Services.logger.println("Encoding is not supported");
			Services.logger.print(e);
			Services.quiter.quit();
			return this;
		}
	}

	public AbstractByteWriter appendVarByteArray(byte[] bytes) throws IOException
	{
		if (bytes == null)
		{
			return append(0);
		}
		else
		{
			if (bytes.length > Services.settings.maxStringSize.get())
			{
				throw new IOException("This byte array is too big. length=" + bytes.length);
			}
			return append(bytes.length).append(bytes);
		}
	}

	public AbstractByteWriter append(PublicKey key) throws IOException
	{
		if (key == null)
		{
			appendVarByteArray(null);
			appendVarByteArray(null);
		}
		else
		{
			appendVarByteArray(((de.flexiprovider.core.rsa.RSAPublicKey) key).getN().toByteArray());
			appendVarByteArray(((de.flexiprovider.core.rsa.RSAPublicKey) key).getE().toByteArray());
		}
		return this;
	}

	public AbstractByteWriter append(double percentComplete) throws IOException
	{
		return append(String.valueOf(percentComplete));
	}
}