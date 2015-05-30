package org.cnv.shr.dmn.mn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.LogWrapper;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		// What happens with two locals by the same name...
		
        // Need to break up chunkList
        // Need to break up PathList

        // get code updater working
        // add setting for ip address
        // start on startup
        // look for column, don't just set value
        // try to start pending downloads
        // set sharing rules
        // show messages
        // make installer
        
        
        // make sure sockets close
        // fix connection/statement cache
        // add download priority
        // make connections show
        // make downloads show
        // figure out why sort on file size doesn't work
        
        // compress data
        // add setting for sync repeat
        
        // black list
        // keep full keys (including date/expire)
        // able to require password to log in.
        
		// look into ExtendedWatchEventModifier
		// monitor free space
        // make standalone key server
		// Need to check versions when messaging.

		
		Arguments a = new Arguments();
		parseArgs(args, a);
		
//		if (true)
//		{
//			a.settings = new Settings(new File("/work/ballin-meme-share/instances/i1/settings.props"));
//		}
//		else
//		{
//			a.settings = new Settings(new File("/work/ballin-meme-share/instances/i2/settings.props"));
//		}

//		if (!a.settings.getSettingsFile().exists())
//		{
//			new NewMachineFrame().setVisible(true);
//			return;
//		}

		try
		{
			Services.initialize(a);
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to initialize", ex);
			if (Services.quiter != null)
			{
				Services.quiter.quit();
			}
		}
	}
	
	private static void parseArgs(String[] args, Arguments a) throws FileNotFoundException, IOException
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-f") && i < args.length - 1)
			{
				a.settings = new Settings(new File(args[i + 1]));
			}

			if (args[i].equals("-u") && i < args.length - 1)
			{
				File directory = new File(args[i+1]);
				if (!directory.exists() || !new File(args[i+1]).isDirectory())
				{
					continue;
				}
				a.updateManagerDirectory = args[i + 1];
			}
		}
	}
	
	public static void restart()
	{
		Services.quiter = new Restart();
		Services.quiter.quit();
	}
}
