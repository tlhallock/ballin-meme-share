package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.json.stream.JsonGenerator;

public final class CountingOutputStream extends OutputStream
{
	private OutputStream delegate;
	private long soFar;
	
	
	

private OutputStream logFile; 
{ 
  Map<String, Object> properties = new HashMap<>(1);
  properties.put(JsonGenerator.PRETTY_PRINTING, true);
	try
	{
		logFile = Files.newOutputStream(Paths.get("log.out." + System.currentTimeMillis() + "." + Math.random() + ".txt"));
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public CountingOutputStream(OutputStream newInputStream)
	{
		this.delegate = newInputStream;
	}
	
	public void stopOtherSide() throws IOException
	{
		delegate.write(13);
		delegate.write(0);
	}

	public long getSoFar()
	{
		return soFar;
	}

	@Override
	public void write(int b) throws IOException
	{
		soFar++;
		delegate.write(b);
		if (b == 13)
		{
			delegate.write(b);
		}
		logFile.write(b);
	}

	public void flush() throws IOException
	{
		delegate.flush();
		logFile.flush();
	}
	
	@Override
	public void close() throws IOException
	{
		delegate.close();
		logFile.close();
	}
}