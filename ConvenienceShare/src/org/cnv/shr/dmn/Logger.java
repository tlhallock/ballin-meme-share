package org.cnv.shr.dmn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Date;

import org.cnv.shr.stng.SettingListener;
import org.cnv.shr.util.CircularOutputStream;
import org.cnv.shr.util.Misc;

public class Logger implements SettingListener
{
	private PrintStream logFile;
	private PrintStream applicationLog;
	private PrintStream sysout;

	public Logger()
	{
		sysout = System.out;
		applicationLog = new PrintStream(new ApplicationLogStream());
		logFile = null;
	}
	
	@Override
	public void settingChanged()
	{
		Services.settings.logFile.addListener(this);
		try
		{
			setLogLocation();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}

	synchronized void setLogLocation() throws IOException
	{
		File file = Services.settings.logFile.get();
		Misc.ensureDirectory(file, true);
		logFile = new PrintStream(new CircularOutputStream(file, 1024 * 1024));
	}
	
	public void printTo(String str, PrintStream ps)
	{
		if (ps != null)
		{
			ps.print(/*new Date() + ":" +*/ str);
		}
	}

	public void print(Exception ex)
	{
		if (ex instanceof SQLException && Services.notifications != null)
		{
			Services.notifications.dbException(ex);
		}
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(byteArrayOutputStream))
		{
			ex.printStackTrace(ps);
		}
		print(new String(byteArrayOutputStream.toString()));
	}

	public void println()
	{
		println("");
	}

	public void println(String str)
	{
		print(str + '\n');
	}

	public void println(Object str)
	{
		print(str.toString() + '\n');
	}

	public synchronized void print(String str)
	{
		printTo(str, sysout);
		printTo(str, applicationLog);
		if (Services.settings.logToFile.get())
		{
			printTo(str, logFile);
		}
	}

	void close()
	{
		logFile.close();
		applicationLog.close();
	}
	
	private class ApplicationLogStream extends OutputStream
	{
		StringBuilder buffer = new StringBuilder();
		
		@Override
		public void write(int arg0) throws IOException
		{
			char c = (char) arg0;
			buffer.append(Character.toString(c));
			
			if (c == '\n' && Services.application != null)
			{
				Services.application.log(buffer.toString());
				buffer.setLength(0);
			}
		}
	}
}
