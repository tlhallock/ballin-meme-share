package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.cnv.shr.stng.SettingListener;
import org.cnv.shr.util.Misc;

public class Logger implements SettingListener
{
	public PrintStream logStream;
	private PrintStream logFile;

	public Logger()
	{
		logStream = new PrintStream(new LogStream());
	}
	
	@Override
	public void settingChanged()
	{
		Services.settings.logFile.addListener(this);
		try
		{
			setLogLocation();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	void setLogLocation() throws FileNotFoundException
	{
		if (!Services.settings.logToFile.get())
		{
			return;
		}
		synchronized (logStream)
		{
			File file = Services.settings.logFile.get();
			Misc.ensureDirectory(file, true);
			logFile = new PrintStream(file);
		}
	}


	void close()
	{
		logStream.close();
		logFile.close();
	}
	
	
	private class LogStream extends OutputStream
	{
		StringBuilder buffer = new StringBuilder();
		
		@Override
		public void write(int arg0) throws IOException
		{
			char c = (char) arg0;
			buffer.append(Character.toString(c));
			System.out.print(c);
			if (logFile != null && Services.settings.logToFile.get())
			{
				logFile.print(c);
			}
			
			if (c == '\n' && Services.application != null)
			{
				Services.application.log(buffer.toString());
				buffer.setLength(0);
			}
		}
	}
}
