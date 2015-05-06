package org.cnv.shr.dmn;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.msg.PathList;

public class Locals
{
	// Sometimes the db can be very slow, cache these for the GUI...
	private HashMap<String, LocalDirectory> memCache = new HashMap<>();
	private HashSet<String> synchronizing = new HashSet<>();
	
	
	public synchronized void share(final File localDirectory)
	{
		Services.userThreads.execute(new Runnable() { public void run()
		{
			try
			{
				Services.logger.logStream.println("Sharing " + localDirectory);
				DbRoots.getLocal(localDirectory.getCanonicalPath()).synchronize(true);
			}
			catch (IOException e1)
			{
				Services.logger.logStream.println("Unable to get file path to share: " + localDirectory);
				e1.printStackTrace(Services.logger.logStream);
			}
		}});
	}
	
	public synchronized boolean startSynchronizing(RootDirectory d)
	{
		return synchronizing.add(d.getCanonicalPath().getFullPath());
	}
	
	public synchronized void stopSynchronizing(RootDirectory d)
	{
		synchronizing.remove(d.getCanonicalPath().getFullPath());
	}
	
	public void share(Communication c)
	{
		c.send(new PathList());
	}

	public LocalFile getLocalFile(String canonicalPath)
	{
		DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
		while (listLocals.hasNext())
		{
			LocalDirectory d = listLocals.next();
			if (!d.contains(canonicalPath))
			{
				continue;
			}
				return d.getFile(canonicalPath);
		}
		return null;
	}

	public void synchronize(final boolean force)
	{
		try
		{
			LinkedList<LocalDirectory> locals = new LinkedList<>();
			final DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
			while (listLocals.hasNext())
			{
				locals.add(listLocals.next());
				
			}
			for (final LocalDirectory local : locals)
			{
				Services.userThreads.execute(new Runnable() {
		            @Override
		            public void run()
		            {
		            	local.synchronize(force);
		            }
				});
			}
//			Services.db.removeUnusedPaths();
		}
		catch (Exception ex)
		{
			Services.logger.logStream.println("Unable to synchronize.");
			ex.printStackTrace(Services.logger.logStream);
		}
	}
	
	/**
	public void read()
	{
		File f = Services.settings.getLocalsFile();
		try (BufferedReader reader = new BufferedReader(new FileReader(f)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				File dir = new File(line);
				if (!dir.exists())
				{
					continue;
				}
				locals.put(line, new LocalDirectory(dir));
			}
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to read Locals.");
			Services.logger.logStream.println("This is expected on first run.");
			e.printStackTrace(Services.logger.logStream);
		}
		
		synchronize();
	}
	**/
	
	public void debug(PrintStream ps)
	{
		DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
		while (listLocals.hasNext())
		{
			ps.println(listLocals.next().getCanonicalPath());
		}
	}
}
