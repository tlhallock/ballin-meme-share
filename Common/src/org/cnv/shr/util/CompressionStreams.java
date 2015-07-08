package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CompressionStreams
{

	public static OutputStream newCompressedOutputStream(PausableOutputStream delegate) throws IOException
	{
		ZipOutputStream zip = new ZipOutputStream(delegate);
		zip.putNextEntry(new ZipEntry(String.valueOf(Math.random())));
		
		OutputStream outerStream = new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				zip.write(b);
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				zip.write(b, off, len);
			}

			@Override
			public void close() throws IOException
			{
				zip.close();
			}
			@Override
			public void flush() throws IOException
			{
				zip.putNextEntry(new ZipEntry(String.valueOf(Math.random())));
				zip.flush();
				delegate.flush();
			}
		};
		return outerStream;
	}

	public static InputStream newCompressedInputStream(PausableInputStream2 delegate) throws IOException
	{
		ZipInputStream zip = new ZipInputStream(delegate);

		InputStream outer = new InputStream()
		{
			@Override
			public int available() throws IOException
			{
				int available;
				do
				{
					available = zip.available();
				}
				while (available == 0 && zip.getNextEntry() != null);
				return available;
			}
			@Override
			public int read() throws IOException
			{
				int read;
				do
				{
					read = zip.read();
				}
				while (read < 0 && zip.getNextEntry() != null);
				return read;
			}
			@Override
			public int read(byte[] buf, int off, int len) throws IOException
			{
				int read;
				do
				{
					read = zip.read(buf, off, len);
				}
				while (read < 0 && zip.getNextEntry() != null);
				return read;
			}
			@Override
			public void close() throws IOException
			{
				zip.close();
				delegate.startAgain();
			}
		};
		return outer;
	}
}
