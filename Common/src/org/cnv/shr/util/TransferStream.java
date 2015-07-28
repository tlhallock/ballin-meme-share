package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;

public class TransferStream
{
	private boolean closed;
	private LinkedBlockingDeque<Integer> buffer = new LinkedBlockingDeque<>();
	
	private InputStream input = new InputStream()
	{
		@Override
		public int read() throws IOException
		{
			if (closed)
			{
				return -1;
			}
			int returnValue;
			try
			{
				returnValue = buffer.takeFirst();
			}
			catch (InterruptedException e)
			{
				throw new IOException("Error while waiting for next byte", e);
			}
			if (returnValue < 0)
			{
				closed = true;
			}
			return returnValue;
		}
		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{
			if (closed)
			{
				return -1;
			}
			
			int first = read();
			if (first < 0)
			{
				return first;
			}
			b[off] = (byte) first;
			
			for (int i = 1; i < len; i++)
			{
				Integer next = buffer.pollFirst();
				if (next == null)
				{
					return i;
				}
				if (next < 0)
				{
					closed = true;
					return i;
				}
				b[off + i] = (byte) next.intValue();
			}
			
			return len;
		}

		@Override
		public int available() throws IOException
		{
			return buffer.size();
		}
		
		public void close()
		{
			closed = true;
		}
	};
	private OutputStream output = new OutputStream()
	{
		@Override
		public void write(int b) throws IOException
		{
			buffer.offerLast(b & 0xff);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException
		{
			for (int i = 0; i < len; i++)
			{
				write(b[off + i]);
			}
		}

		@Override
		public void close() throws IOException
		{
			buffer.offerLast(-1);
		}
	};
	
	public InputStream getInput()
	{
		return input;
	}
	
	public OutputStream getOutput()
	{
		return output;
	}
}
