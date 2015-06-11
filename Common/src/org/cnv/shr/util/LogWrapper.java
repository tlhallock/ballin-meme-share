
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */


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
