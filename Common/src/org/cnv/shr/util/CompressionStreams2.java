package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.cnv.shr.util.HardToCloseStreams.HardToCloseInputStream;
import org.cnv.shr.util.HardToCloseStreams.HardToCloseOutputStream;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;


public class CompressionStreams2
{
	private static final class ZipStats
	{
		long uncompressed;
		long compressed;
		public String toString()
		{
			return "compression ratio: " + compressed / (double) uncompressed;
		}
	}

	public static HardToCloseOutputStream newCompressedOutputStream(PausableOutputStream delegate) throws IOException
	{
		ZipStats stats = new ZipStats();
		delegate.stopOtherSide();
		OutputStream zip = new SnappyFramedOutputStream(new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				delegate.write(b);
				stats.compressed++;
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				delegate.write(b, off, len);
				stats.compressed+=len;
			}

			@Override
			public void close() throws IOException
			{
				delegate.close();
			}
			@Override
			public void flush() throws IOException
			{
				delegate.flush();
			}
		});
		HardToCloseOutputStream outerStream = new HardToCloseOutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				zip.write(b);
				stats.uncompressed++;
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				zip.write(b, off, len);
				stats.uncompressed+=len;
				
				System.out.println("Compressed " + new String(b, off, len));
			}

			@Override
			public void close() throws IOException
			{
				zip.close();
				LogWrapper.getLogger().info(stats.toString());
			}
			@Override
			public void flush() throws IOException
			{
				zip.flush();
				delegate.flush();
				LogWrapper.getLogger().info(stats.toString());
				System.out.println("Flushed");
			}
			@Override
			public void actuallyClose() throws IOException
			{
				close();
				delegate.actuallyClose();
			}
		};
		return outerStream;
	}

	public static HardToCloseInputStream newCompressedInputStream(PausableInputStream2 delegate) throws IOException
	{
		delegate.startAgain();
		InputStream zip = new SnappyFramedInputStream(delegate, false);

		HardToCloseInputStream outer = new HardToCloseInputStream()
		{
			@Override
			public int available() throws IOException
			{
				return zip.available();
			}
			@Override
			public int read() throws IOException
			{
				return zip.read();
			}
			@Override
			public int read(byte[] buf, int off, int len) throws IOException
			{
				return zip.read(buf, off, len);
			}
			@Override
			public void close() throws IOException
			{
				zip.close();
				delegate.startAgain();
			}
			@Override
			public void actuallyClose() throws IOException
			{
				zip.close();
				delegate.actuallyClose();
			}
		};
		return outer;
	}
}
