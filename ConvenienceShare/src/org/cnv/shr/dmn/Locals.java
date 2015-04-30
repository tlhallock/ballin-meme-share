package org.cnv.shr.dmn;

import java.io.File;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;

public class Locals
{
	public synchronized void share(File localDirectory)
	{
		final LocalDirectory local = new LocalDirectory(Services.localMachine, localDirectory);
		try
		{
			Services.db.addRoot(Services.localMachine, local);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		Services.userThreads.execute(new Runnable() { public void run()
		{
			local.synchronize();
		}});
	}

	public synchronized List<LocalDirectory> listLocals()
	{
		return Services.db.getLocals();
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
			ps.println(path.getPath());
		}
	}
}
