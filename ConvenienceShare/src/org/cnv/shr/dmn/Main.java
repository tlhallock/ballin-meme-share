package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import org.cnv.shr.stng.Settings;
import org.cnv.shr.test.TestActions;
import org.cnv.shr.util.Misc;

public class Main
{
	private static class Arguments
	{
		// needs to have a quitting and testing...
		boolean connectedToTestStream = false;
		boolean deleteDb = false;
		Settings settings;
	}
	// Need to fix these...
	public static boolean quitting;
	public static boolean testing;
	
	// should be a terminator object...
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
			Arguments a = parseArgs(args);
			a.settings.read();
			// should accept an arguments
			Services.initialize(a.settings, a.deleteDb);
			checkForTestStream(args, a);
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
			quit();
		}
	}
	
	private static Arguments parseArgs(String[] args) throws FileNotFoundException, IOException
	{
		final Arguments a = new Arguments();
		a.settings = new Settings(new File("/work/ballin-meme-share/runDir/settings1.props"));
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-f") && i < args.length - 1)
			{
				a.settings = new Settings(new File(args[i + 1]));
			}
			
			// put in MainTest...
			if (args[i].equals("-d"))
			{
				a.deleteDb = true;
			}
			if (args[i].equals("-k") && i < args.length - 1)
			{
				new Timer().schedule(new TimerTask() {
					@Override
					public void run()
					{
						if (!a.connectedToTestStream)
						{
							quit();
						}
					}}, Long.parseLong(args[i+1]));
			}
		}
		return a;
	}
	private static void checkForTestStream(String[] args, Arguments a) throws InterruptedException, Exception, IOException, UnknownHostException
	{
		// Put in mainTest...
		for (int i = 0; i < args.length; i++)
		{
			if (!args[i].equals("-t") || i >= args.length - 2)
			{
				continue;
			}
			handleTestStream(a, args[i+1], args[i+2]);
		}
	}
	private static void handleTestStream(Arguments a, String url, String port) throws InterruptedException, Exception, IOException, UnknownHostException
	{
		try (Socket socket = new Socket(url, Integer.parseInt(port));
		     ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());)
		{
				a.connectedToTestStream = true;
				Thread.sleep(2000);
				TestActions.run(reader);
		}
		quit();
	}

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
			// should be subclass
			if (testing)
			{
				return;
			}
			System.exit(0);
		}}
}
