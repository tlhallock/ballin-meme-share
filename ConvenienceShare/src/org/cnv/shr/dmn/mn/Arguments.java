package org.cnv.shr.dmn.mn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import org.cnv.shr.stng.Settings;

public class Arguments
{
	// needs to have a quitting and testing...
	public boolean connectedToTestStream = false;
	public boolean deleteDb = false;
	public boolean showGui = false;
	public Settings settings = new Settings(Settings.DEFAULT_SETTINGS_FILE);
	public Quiter quiter = new Quiter() {
		@Override
		public void doFinal()
		{
			System.exit(0);
		}};
	String testIp;
	String testPort;
	
	public String updateManagerDirectory;
	

	void parseArgs(String[] args) throws FileNotFoundException, IOException
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-d"))
			{
				deleteDb = true;
			}
			if (args[i].equals("-g"))
			{
				showGui = true;
			}
			if (args[i].equals("-f") && i < args.length - 1)
			{
				settings = new Settings(Paths.get(args[i + 1]));
			}

			if (args[i].equals("-u") && i < args.length - 1)
			{
				File directory = new File(args[i+1]);
				if (!directory.exists() || !new File(args[i+1]).isDirectory())
				{
					continue;
				}
				updateManagerDirectory = args[i + 1];
			}
		}
	}
}