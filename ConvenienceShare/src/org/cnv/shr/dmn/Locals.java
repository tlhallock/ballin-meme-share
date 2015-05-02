package org.cnv.shr.dmn;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.FileList;

public class Locals
{
	// Sometimes the db can be very slow, cache these for the GUI...
	private HashMap<String, LocalDirectory> memCache = new HashMap<>();
	
	
	public synchronized void share(final File localDirectory)
	{
		Services.userThreads.execute(new Runnable() { public void run()
		{
			try
			{
				Services.logger.logStream.println("Sharing " + localDirectory);
				new LocalDirectory(localDirectory).synchronize(true);
			}
			catch (IOException e1)
			{
				Services.logger.logStream.println("Unable to get file path to share: " + localDirectory);
				e1.printStackTrace(Services.logger.logStream);
			}
		}});
	}
	
	public void share(Connection c)
	{
		FileList msg = new FileList();
		int count = 0;
		for (LocalDirectory local : listLocals())
		{
			Iterator<SharedFile> list = local.list();
			while (list.hasNext())
			{
				SharedFile file = list.next();
				count++;
				msg.add(local, file);
				
				if (count > 50)
				{
					c.send(msg);
					count = 0;
					msg = new FileList();
				}
			}
		}
		if (count > 1)
		{
			c.send(msg);
		}
	}
	
	public boolean localAlreadyExists(String canonicalPath)
	{
		for (LocalDirectory other : listLocals())
		{
			if (other.getCanonicalPath().equals(canonicalPath))
			{
				return true;
			}
		}
		return false;
	}

	public List<LocalDirectory> listLocals()
	{
		return Services.db.getLocals();
	}

	public LocalFile getLocalFile(File f)
	{
		for (LocalDirectory d : listLocals())
		{
			if (!d.contains(f))
			{
				continue;
			}
			try
			{
				return d.getFile(f.getCanonicalPath());
			}
			catch (IOException e)
			{
				Services.logger.logStream.println("Unable to get file path: " + f);
				e.printStackTrace(Services.logger.logStream);
			}
		}
		return null;
	}

	public void synchronize(boolean force)
	{
		for (LocalDirectory localDir : listLocals())
		{
			localDir.synchronize(force);
		}
		Services.db.removeUnusedPaths();
		
		// Right now this is only for the sizes of the local dirs.
		Services.notifications.localsChanged();
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
		for (LocalDirectory path : listLocals())
		{
			ps.println(path.getCanonicalPath());
		}
	}
}
