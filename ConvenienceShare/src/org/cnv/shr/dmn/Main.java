package org.cnv.shr.dmn;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;

import org.cnv.shr.stng.Settings;
import org.cnv.shr.test.TestActions;
import org.cnv.shr.util.Misc;

public class Main
{
	public static void main(String[] args) throws Exception
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

		try
		{
			for (int i = 0; i < args.length; i++)
			{
				if (args[i].equals("-t") && i < args.length - 2)
				{
					try (Socket socket = new Socket(args[i+1], Integer.parseInt(args[i+2])))
					{
						TestActions.run(new BufferedReader(new InputStreamReader(socket.getInputStream())));
					}
					return;
				}
				if (args[i].equals("-f") && i < args.length - 1)
				{
					Settings settings = new Settings(new File(args[i + 1]));
					settings.read();
					Services.initialize(settings);
					return;
				}
			}

			Settings settings = new Settings(new File("/work/ballin-meme-share/runDir/settings1.props"));
			settings.read();
			Services.initialize(settings);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			quit();
		}
	}

	private static boolean quitting = false;
	public static void quit()
	{
		if (quitting)
		{
			return;
		}
		else
		{
			quitting = true;
		}
		try
		{
			Services.deInitialize();
		}
		finally
		{
			System.exit(0);
		}
	}
}
