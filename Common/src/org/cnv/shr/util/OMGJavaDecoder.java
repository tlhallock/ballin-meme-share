package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class OMGJavaDecoder extends Reader
{
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  
  private CharsetDecoder decoder = UTF_8.newDecoder();
  private InputStream delegate;
  
	private ByteBuffer byteBuffer;
	private CharBuffer charBuffer;
	
	private boolean closed;
  
  public OMGJavaDecoder(InputStream input, int bufferSize)
  {
  	byteBuffer = ByteBuffer.wrap(new byte[bufferSize]);
  	charBuffer = CharBuffer.allocate(bufferSize);
  	
  	this.delegate = input;
  	charBuffer.flip();
  	byteBuffer.flip();
  }
  
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException
	{
		for (;;)
		{
			int numRemaining = charBuffer.remaining();
			if (numRemaining > 0)
			{
				if (len > numRemaining)
					len = numRemaining;
				charBuffer.get(cbuf, off, len);
				return len;
			}
			if (closed) return -1;
			
			if (byteBuffer.hasRemaining())
			{
				charBuffer.compact();
				decoder.decode(byteBuffer, charBuffer, false);
				charBuffer.flip();
				if (charBuffer.hasRemaining())
				{
					continue;
				}
			}

			byteBuffer.compact();
			int nread = delegate.read(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
			if (nread < 0)
			{
				closed = true;

				byteBuffer.flip();
				charBuffer.compact();
				
				decoder.decode(byteBuffer, charBuffer, true);
				charBuffer.flip();
				
				byteBuffer.compact();
				continue;
			}

			byteBuffer.position(byteBuffer.position() + nread);
			byteBuffer.flip();

			charBuffer.compact();
			decoder.decode(byteBuffer, charBuffer, false);
			charBuffer.flip();
		}
	}

	@Override
	public void close() throws IOException
	{
		delegate.close();
	}
}
