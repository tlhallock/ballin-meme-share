package org.cnv.shr.dmn.mn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.Misc;

public class Main
{
	// not static for now
	public void main(String[] args) throws Exception
	{
        System.out.println("Starting from " + Misc.getJarPath());
		
		// look into ExtendedWatchEventModifier
		// all syncs on the same thread
		// monitor free space
		// add temp   <---------------      ?????????
		// make sure downloaded files are in a mirror...
		
		
		
		// Right now the locals are not showing their respective number of files and total file sizes because they are not being written to the db.
		
		// black list
		// larger auth in first message
		// make standalone key server
		// make database text size correct

		// add setting for checksum
		// add setting for ip address
		// add setting for sync repeat
		
		// start on startup

		Arguments a = new Arguments();
		parseArgs(args, a);
		try
		{
			Services.initialize(a);
		}
		catch (Exception ex)
		{
			if (Services.logger != null)
			{
				Services.logger.print(ex);
			}
			else
			{
				ex.printStackTrace();
			}
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
		}
	}
}
