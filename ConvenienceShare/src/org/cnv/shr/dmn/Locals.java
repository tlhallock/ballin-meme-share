package org.cnv.shr.dmn;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;

public class Locals
{
	public synchronized void share(final File localDirectory)
	{
		Services.userThreads.execute(new Runnable() { public void run()
		{
			try
			{
				Services.logger.logStream.println("Sharing " + localDirectory);
				new LocalDirectory(localDirectory).synchronize();
			}
			catch (IOException e1)
			{
				Services.logger.logStream.println("Unable to get file path to share: " + localDirectory);
				e1.printStackTrace(Services.logger.logStream);
			}
		}});
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

	public synchronized List<LocalDirectory> listLocals()
	{
		return Services.db.getLocals();
	}

	public synchronized LocalFile getLocalFile(File f)
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

	public void synchronize()
	{
		for (LocalDirectory localDir : listLocals())
		{
			localDir.synchronize();
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
