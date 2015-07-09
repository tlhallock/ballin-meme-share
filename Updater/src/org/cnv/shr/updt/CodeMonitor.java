package org.cnv.shr.updt;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class CodeMonitor implements Runnable
{
	private Path directory;
	private WatchService service;
	
	private boolean quit = false;
	
	public CodeMonitor(Path root) throws IOException
	{
		this.directory = root;
		service = root.getFileSystem().newWatchService();
	}
	
	public void quit()
	{
		quit = true;
	}
	
	public void run()
	{
		Misc.ensureDirectory(directory, false);
		WatchKey register;
		try
		{
			register = directory.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to watch directory " + directory, e1);
			return;
		}
		while (!quit)
		{
			WatchKey watchKey;
			try
			{
				watchKey = service.poll(60, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted", ex);
				break;
			}
			if (watchKey == null)
			{
				continue;
			}
			
			if (!watchKey.pollEvents().isEmpty())
			{
				Updater.code.checkTime();
			}

			watchKey.reset();
		}

		register.cancel();
	}
}
