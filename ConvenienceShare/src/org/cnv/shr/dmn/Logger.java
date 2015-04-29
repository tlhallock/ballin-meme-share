package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.cnv.shr.util.Misc;

public class Logger
{
	public PrintStream logStream;

	public Logger()
	{
		logStream = System.out;
	}

	void setLogLocation() throws FileNotFoundException
	{
		File file = Services.settings.getLogFile();
		Misc.ensureDirectory(file, true);
		logStream = new PrintStream(file);
	}


	void close()
	{
		logStream.flush();
	}
}
