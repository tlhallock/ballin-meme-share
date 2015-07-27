package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketStreams
{
	// I get exceptions when I close a JsonGenerator and a JsonParser on the same socket, because whichever came first closes the socket when it closes.
	// These wrap the output stream with a call to shutdownOuput/Input instead.
	
	public static OutputStream newSocketOutputStream(Socket socket) throws IOException
	{
		OutputStream delegate = socket.getOutputStream();
		OutputStream outputStream = new OutputStream()
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
				socket.shutdownOutput();
			}
			@Override
			public void flush() throws IOException
			{
				delegate.flush();
			}
		};
		return outputStream;
	}

	public static InputStream newSocketInputStream(Socket socket) throws IOException
	{
		InputStream delegate = socket.getInputStream();
		InputStream inputStream = new InputStream()
		{
			@Override
			public int read() throws IOException
			{
				return delegate.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException
			{
				return delegate.read(b, off, len);
			}

			@Override
			public int available() throws IOException
			{
				return delegate.available();
			}

			@Override
			public void close() throws IOException
			{
				socket.shutdownInput();
			}

			@Override
			public synchronized void mark(int readlimit)
			{
				delegate.mark(readlimit);
			}

			@Override
			public boolean markSupported()
			{
				return delegate.markSupported();
			}

			@Override
			public synchronized void reset() throws IOException
			{
				delegate.reset();
			}

			@Override
			public long skip(long n) throws IOException
			{
				return delegate.skip(n);
			}
		};
		return inputStream;
	}
}
