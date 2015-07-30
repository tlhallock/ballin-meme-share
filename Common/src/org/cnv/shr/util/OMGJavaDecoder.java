package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
  
  CharBuffer charBuffer = CharBuffer.allocate(1024);
  private byte[] bytes = new byte[1024];
	ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 10);
	
	private boolean closed;
  
  public OMGJavaDecoder(InputStream input)
  {
  	this.delegate = input;
  	charBuffer.flip();
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
				System.out.println(charBuffer.get(cbuf, off, len));
				System.out.println(charBuffer);
				return len;
			}
			if (closed) return -1;

			System.out.println(charBuffer); //charBuffer.compact();
			System.out.println(charBuffer.clear()); //charBuffer.compact();
			System.out.println(charBuffer); //charBuffer.compact();

			int nread = delegate.read(bytes, 0, bytes.length);
			if (nread < 0)
			{
				closed = true;
				System.out.println(decoder.decode(byteBuffer, charBuffer, true));
				System.out.println(charBuffer.flip());
				
				System.out.println(byteBuffer.clear()); // byteBuffer.compact();
				continue;
			}

			System.out.println(byteBuffer.put(bytes, 0, nread));
			System.out.println(byteBuffer.flip());

			System.out.println(decoder.decode(byteBuffer, charBuffer, false));
			System.out.println(charBuffer.flip());

			System.out.println(byteBuffer.clear()); // byteBuffer.compact();
		}
	}

	@Override
	public void close() throws IOException
	{
		delegate.close();
	}
	
	
	public static void main(String[] args) throws IOException
	{
		TransferStream tranferStream = new TransferStream();
		BufferedReader reader = new BufferedReader(new OMGJavaDecoder(new ByteArrayInputStream("here\nis a few different\nlines\n".getBytes())));
		
		
		String line;
		while ((line = reader.readLine()) != null)
		{
			System.out.println(line);
		}
	}
}
