package org.cnv.shr.dmn.mn;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.ShowApplication;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.OutputByteWriter;

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
		
		a.settings = new Settings(Paths.get("/work/ballin-meme-share/instances/i1/settings.props"));
		a.settings.servePortBeginE.set(9990);
		a.settings.servePortBeginI.set(9990);

		System.out.println("Settings file: " + a.settings.getSettingsFile());
		
		if (Services.isAlreadyRunning(a))
		{
			LogWrapper.getLogger().info("Application must already be running.");
			String address = InetAddress.getLocalHost().getHostAddress();
			try (Socket socket = new Socket(address, a.settings.servePortBeginI.get());
					InputStream input = socket.getInputStream();
					OutputStream outputStream = socket.getOutputStream();
					OutputByteWriter outputByteWriter = new OutputByteWriter(outputStream);)
			{
				new ShowApplication().write(null, outputByteWriter);
				new DoneMessage().write(null, outputByteWriter);
				outputStream.flush();
				LogWrapper.getLogger().info("Message sent. Waiting...");
				Thread.sleep(10000);
			}
			return;
		}
		
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
		
		
//		for (;;)
//		{
//			synchronized (System.out)
//			{
//				Thread.sleep(1000);
//			}
//			Thread.sleep(1000);
//		}
	}
	
	
	public static void restart()
	{
		Services.quiter = new Restart();
		Services.quiter.quit();
	}
}
