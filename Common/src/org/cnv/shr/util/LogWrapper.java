package org.cnv.shr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogWrapper
{
	private static Logger logger = Logger.getGlobal();
	static {
		logger.setUseParentHandlers(false);
		logger.addHandler(new ConsoleHandler() {
			{
				setOutputStream(System.out);
				setFormatter(new Formatter() {
					@Override
					public String format(LogRecord record)
					{
		        String throwable = "";
		        if (record.getThrown() != null) {
		            StringWriter sw = new StringWriter();
		            try (PrintWriter pw = new PrintWriter(sw);) {
									pw.println();
									record.getThrown().printStackTrace(pw);
									pw.close();
								}
		            throwable = sw.toString();
		        }
		        return record.getMessage() + "\n" + throwable;
					}});
			}
		});
	}
	
	
	
	
	private static Handler fileHandler;

	// This should never be created...
	private LogWrapper() throws Exception { throw new Exception("Do not create the wrapper..."); }

	public static synchronized void logToFile(final Path file, final long logLength)
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
			logger.addHandler(fileHandler = createFileHandler(new CircularOutputStream(file, logLength)));
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

	public static void close()
	{
		fileHandler.close();
	}
}
