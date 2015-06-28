package org.cnv.shr.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;

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
		logFile.write(b); logFile.flush();
		if (b == PausableInputStream.PAUSE_BYTE && !rawMode)
		{
			// Escape the pause byte
			delegate.write(PausableInputStream.PAUSE_BYTE);
		}
	}
	public void flush() throws IOException
	{
		delegate.flush();
		logFile.flush();
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
		logFile.write("<paused here>"); logFile.flush();
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
	
	
	
	
	
	
	
	
	

private BufferedWriter logFile; 
{ 
  Map<String, Object> properties = new HashMap<>(1);
  properties.put(JsonGenerator.PRETTY_PRINTING, true);
	try
	{
		String string = "log.out." + System.currentTimeMillis() + "." + Math.random() + ".txt";
		System.out.println("Logging to " + string);
		logFile = Files.newBufferedWriter(Paths.get(string));
	}
	catch (IOException e)
	{
		LogWrapper.getLogger().log(Level.INFO, "Unable to log to file", e);
	}
}
}
