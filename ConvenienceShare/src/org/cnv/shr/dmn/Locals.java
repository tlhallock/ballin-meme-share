package org.cnv.shr.dmn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;

public class Locals
{
	private HashMap<String, LocalDirectory> locals = new HashMap<>();
	
	public synchronized void share(File localDirectory)
	{
		if (locals.containsKey(localDirectory))
		{
			return;
		}
		
		String path = localDirectory.getAbsolutePath(); 
		final LocalDirectory local = new LocalDirectory(localDirectory);
		locals.put(path, local);
		Services.userThreads.execute(new Runnable() { public void run()
		{
			local.synchronize();
		}});
	}

	public synchronized List<LocalDirectory> listLocals()
	{
		return new LinkedList<LocalDirectory>(locals.values());
	}

	public synchronized LocalFile getLocalFile(File f)
	{
		for (LocalDirectory d : listLocals())
		{
			if (d.contains(f))
			{
				return d.getFile(f.getAbsolutePath());
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
		Notifications.localsChanged();
	}
	
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
	
	public void write()
	{
		File f = Services.settings.getLocalsFile();
		try (PrintStream ps = new PrintStream(new FileOutputStream(f)))
		{
			for (String path : locals.keySet())
			{
				ps.println(path);
			}
		}
		catch (FileNotFoundException e)
		{
			Services.logger.logStream.println("Unable to save Locals.");
			e.printStackTrace(Services.logger.logStream);
		}
	}
}
