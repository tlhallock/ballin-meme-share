package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class LogStreams
{
	public static OutputStream newLogOutputStream(OutputStream output, String prefix) throws IOException
	{
		Path randomName = getRandomName("out." + prefix);
		LogWrapper.getLogger().info("Loggoing stream to " + randomName);
		return newLogOutputStream(output, randomName);
	}
	
	public static OutputStream newLogOutputStream(OutputStream output, Path file) throws IOException
	{
		OutputStream log = Files.newOutputStream(file);
		
		OutputStream returnValue = new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				output.write(b);
				log.write(b); log.flush();
			}
			
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				output.write(b, off, len);
				log.write(b, off, len); log.flush();
			}
			
			@Override
			public void close() throws IOException
			{
				output.close();
				log.close();
			}
			@Override
			public void flush() throws IOException
			{
				output.flush();
			}
		};
		return returnValue;
	}

	public static InputStream newLogInputStream(InputStream input, String prefix)
	{
		return newLogInputStream(input, getRandomName("in." + prefix));
	}

	private static Path getRandomName(String prefix)
	{
		return Paths.get(prefix + "." + System.currentTimeMillis() + Math.random() + ".txt");
	}
	
	private static InputStream newLogInputStream(InputStream input, Path file)
	{
		OutputStream log;
		try
		{
			log = Files.newOutputStream(file);
		}
		catch (Exception x)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create log file.", x);
			return input;
		}
		
		InputStream inputStream = new InputStream()
		{
			@Override
			public int read() throws IOException
			{
				int read = input.read();
				if (read > 0)
				{
					log.write(read); log.flush();
				}
				return read;
			}
			@Override
			public int read(byte[] b, int off, int len) throws IOException
			{
				int read = input.read(b, off, len);
				if (read > 0)
				{
					log.write(b, off, read); log.flush();
				}
				return read;
			}
			@Override
			public void close() throws IOException
			{
				input.close();
				log.close();
			}
			@Override
			public int available() throws IOException
			{
				return input.available();
			}
			@Override
			public long skip(long n) throws IOException
			{
				return input.skip(n);
			}
			@Override
			public boolean markSupported()
			{
				return input.markSupported();
			}
			@Override
			public synchronized void mark(int readlimit)
			{
				input.mark(readlimit);
			}
			@Override
			public synchronized void reset() throws IOException
			{
				input.reset();
			}
		};
		return inputStream;
	}
}
