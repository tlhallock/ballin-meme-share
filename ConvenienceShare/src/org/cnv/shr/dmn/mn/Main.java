package org.cnv.shr.dmn.mn;

import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		// What happens with two locals by the same name...
		// Make kill frame if already running...
		// remove all caching of db objects...
		
        // Need to break up chunkList
        // Need to break up PathList

        // get code updater working
			// get simple tracker (just list machines)
		
		
        // start on startup
        // try to start pending downloads
        // set sharing rules
        // show messages
        // make installer
        
		// IOUtils.copy
        
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
		a.parseArgs(args);
		
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
	
	
	public static void restart()
	{
		Services.quiter = new Restart();
		Services.quiter.quit();
	}
}
