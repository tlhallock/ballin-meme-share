package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionStreams
{

	public static OutputStream newCompressedOutputStream(long numBytes, OutputStream delegate) throws IOException
	{
		OutputStream innerStream = new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				delegate.write(b);
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				delegate.write(b, off, len);
			}
			@Override
			public void close() throws IOException
			{
				flush();
			}
			@Override
			public void flush() throws IOException
			{
				delegate.flush();
			}
		};
		
		GZIPOutputStream zip = new GZIPOutputStream(innerStream);
		
		OutputStream outerStream = new OutputStream()
		{
			boolean closed;
			long remaining = numBytes;
			@Override
			public void write(int b) throws IOException
			{
				if (remaining <= 0)
				{
					LogWrapper.getLogger().warning("Wrote past the number of bytes expected to write!!\nWriting directly to stream...");
					close();
					delegate.write(b);
					return;
				}
				zip.write(b);
				remaining--;
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				if (len <= remaining)
				{
					zip.write(b, off, len);
					remaining -= len;
					return;
				}
				
				int newLength = (int) remaining;
				LogWrapper.getLogger().warning("Wrote " + (len - newLength) + " past the number of bytes expected to write!!\nWriting directly to stream...");
				zip.write(b, off, newLength);
				close();
				delegate.write(b, off + newLength, len - newLength);
				remaining = 0;
				return;
			}

			@Override
			public void close() throws IOException
			{
				if (!closed)
				{
					zip.close();
				}
			}
			@Override
			public void flush() throws IOException
			{
				if (!closed)
				{
					zip.flush();
				}
				else
				{
					delegate.flush();
				}
			}
		};
		return outerStream;
	}

	public static InputStream newCompressedInputStream(long numBytes, InputStream input) throws IOException
	{
		return new GZIPInputStream(new InputStream()
		{
			long remaining = numBytes;
			@Override
			public int available() throws IOException
			{
				int available = input.available();
				if (remaining < available)
				{
					return (int) remaining;
				}
				return available;
			}
			@Override
			public int read() throws IOException
			{
				if (remaining <= 0)
				{
					return -1;
				}
				remaining--;
				return input.read();
			}
			@Override
			public int read(byte[] buf, int off, int len) throws IOException
			{
				if (remaining <= 0)
				{
					return -1;
				}
				if (len > remaining)
				{
					len = (int) remaining;
				}
				int read = input.read(buf, off, len);
				remaining -= read;
				return read;
			}
			@Override
			public void close() throws IOException
			{
				// finish the remaining bytes...
				while (remaining > 0)
				{
					input.read();
					remaining--;
				}
			}
		});
	}
	
	public static void main(String[] args) throws IOException
	{
		byte[] bytes = "here is some text \nto be compressed.\n".getBytes();
		LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<Integer>();
		
		new Thread() {
			public void run()
			{
				try (InputStream input2 = new InputStream()
				{
					boolean done;
					@Override
					public int read() throws IOException
					{
						if (done)
						{
							return -1;
						}
						try
						{
							Integer takeFirst = queue.takeFirst();
							if (takeFirst < 0)
							{
								done = true;
							}
							System.out.println("\t\t\t\tRead " + takeFirst);
							return takeFirst;
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
							return -1;
						}
					}

					public int available()
					{
						if (done)
						{
							return 0;
						}
						return queue.size();
					}
				};)
				{
					try (BufferedReader input = new BufferedReader(new InputStreamReader(newCompressedInputStream(bytes.length, input2))))
					{
						String line;
						while ((line = input.readLine()) != null)
						{
							System.out.println(line);
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}.start();
		
		try (OutputStream outputStream = new OutputStream()
				{
					@Override
					public void write(int b) throws IOException
					{
						queue.offerLast(b & 0xff);
						System.out.println("Wrote " + (b & 0xff));
					}
					public void close()
					{
						queue.offerLast(-1);
					}
				};)
		{
			try (OutputStream output = newCompressedOutputStream(bytes.length, outputStream);)
			{
				output.write(bytes);
			}
		}
	}
}
