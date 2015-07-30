package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The json parser continues to read after the current object.
 * When I change streams, this messes up the json.
 * To prevent this, I pause the stream with this class.
 * 
 * It has a special byte that signals the end of the current stream, so that we don't read past it.
 */
public class PausableOutputStream extends OutputStream
{
	private OutputStream delegate;
	boolean rawMode;

	public PausableOutputStream(OutputStream delegate)
	{
		this.delegate = delegate;
	}
	
	public void setDelegate(OutputStream delegate)
	{
		this.delegate = delegate;
	}
	
	@Override
	public void write(int b) throws IOException
	{
		delegate.write(b);
		if (b == PausableInputStream.PAUSE_BYTE && !rawMode)
		{
			// Escape the pause byte
			delegate.write(PausableInputStream.PAUSE_BYTE);
		}
	}
	public void flush() throws IOException
	{
		delegate.flush();
	}
	
	public void setRawMode(boolean rawMode)
	{
		this.rawMode = rawMode;
	}
	
	public void stopOtherSide() throws IOException
	{
		// Do not escape the pause byte
		delegate.write(PausableInputStream.PAUSE_BYTE);
		delegate.write(0);
	}
	
	@Override
	public void close() throws IOException
	{
		stopOtherSide();
		flush();
	}
	
	/*
	 * Should be called...
	 */
	public void actuallyClose() throws IOException
	{
		delegate.close();
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (rawMode)
		{
			delegate.write(b, off, len);
			return;
		}
		
		int end = off + len;
		for (int i = off; i < end; i++)
		{
			write(b[i]);
		}
	}
}
