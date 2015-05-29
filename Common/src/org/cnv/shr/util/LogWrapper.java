package org.cnv.shr.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogWrapper
{
	private static Logger logger = Logger.getGlobal();
	private static Handler fileHandler;

	// This should never be created...
	private LogWrapper() throws Exception { throw new Exception("Do not create the wrapper..."); }

	public static synchronized void logToFile(final File file, final long logLength)
	{
		if (fileHandler != null)
		{
			fileHandler.close();
		}
		if (file == null || logLength < 0)
		{
			fileHandler = null;
			return;
		}
		try
		{
			Misc.ensureDirectory(file, true);
			CircularOutputStream output = new CircularOutputStream(file, logLength);
			logger.addHandler(fileHandler = createFileHandler(output));
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}

	private static Handler createFileHandler(final CircularOutputStream output)
	{
		return new Handler()
		{
			private SimpleFormatter formatter = new SimpleFormatter();

			@Override
			public void close() throws SecurityException
			{
				try
				{
					output.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			@Override
			public void flush()
			{
				try
				{
					output.flush();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			@Override
			public void publish(LogRecord record)
			{
				try
				{
					output.write(formatter.format(record).getBytes());
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
	}
	public static Logger getLogger()
	{
		return logger;
	}
}
