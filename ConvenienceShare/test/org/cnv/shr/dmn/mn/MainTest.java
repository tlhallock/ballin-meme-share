package org.cnv.shr.dmn.mn;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.test.TestActions;
import org.cnv.shr.util.LogWrapper;

public class MainTest
{
	public static void main(String[] args)
	{
		Arguments a = new Arguments();
		parseArgs(args, a);
		try
		{
			Services.isAlreadyRunning(a);
			Services.initialize(a);
			if (a.testIp != null && a.testPort != null)
			{
				handleTestStream(a);
			}
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Unable to initialize", ex);
			Services.quiter.quit();
		}
	}
	
	private static void parseArgs(String[] args, final Arguments a)
	{
		for (int i = 0; i < args.length; i++)
		{
			// put in MainTest...
			if (args[i].equals("-d"))
			{
				a.deleteDb = true;
			}
			else if (args[i].equals("-k") && i < args.length - 1)
			{
				new Timer().schedule(new TimerTask() {
					@Override
					public void run()
					{
						if (!a.connectedToTestStream)
						{
							Services.quiter.quit();
						}
					}
				}, Long.parseLong(args[i+1]));
			}
			else if (args[i].equals("-f") && i < args.length - 1)
			{
				a.settings = new Settings(Paths.get(args[i + 1]));
			}
			else if (args[i].equals("-t") && i < args.length - 2)
			{
				a.testIp = args[i+1];
				a.testPort = args[i+2];
			}
		}
	}

	private static void handleTestStream(Arguments a) throws InterruptedException, Exception, IOException, UnknownHostException
	{
		try (Socket socket = new Socket(a.testIp, Integer.parseInt(a.testPort));
		     ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());)
		{
				a.connectedToTestStream = true;
				Thread.sleep(2000);
				TestActions.run(reader);
		}
		Services.quiter.quit();
	}
}
