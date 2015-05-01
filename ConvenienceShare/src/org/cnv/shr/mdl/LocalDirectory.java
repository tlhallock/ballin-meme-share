package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Find;

public class LocalDirectory extends RootDirectory
{
	public LocalDirectory(File localDirectory) throws IOException
	{
		super(Services.localMachine, localDirectory.getCanonicalPath());
	}
	
	public boolean contains(File f)
	{
		try
		{
			return f.getCanonicalPath().startsWith(path);
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to get file path: " + f);
			e.printStackTrace(Services.logger.logStream);
			return false;
		}
	}
	
	private boolean prune()
	{
		boolean changed = false;
		Iterator<SharedFile> currentLocals = Services.db.list(this);
		while (currentLocals.hasNext())
		{
			LocalFile local = (LocalFile) currentLocals.next();
			changed |= local.refreshAndWriteToDb();
		}
		return changed;
	}
	
	private boolean search()
	{
		boolean changed = false;
		Find find = new Find(new File(path));
		while (find.hasNext())
		{
			File f = find.next();

			Services.logger.logStream.println("Found file " + f);
			String absolutePath;
			try
			{
				absolutePath = f.getCanonicalPath();
			}
			catch (IOException e)
			{
				Services.logger.logStream.println("Unable to get file path: " + f);
				e.printStackTrace(Services.logger.logStream);
				continue;
			}

			if (Services.db.findLocalFile(this, f) != null)
			{
				continue;
			}

			Services.db.addFile(this, new LocalFile(getThis(), absolutePath));
			changed = true;
		}
		return changed;
	}

	@Override
	public void synchronizeInternal()
	{
		if (!Services.locals.localAlreadyExists(getCanonicalPath()) && !Services.db.addRoot(Services.localMachine, this))
		{
			return;
		}
		
		Services.logger.logStream.println("Synchronizing " + getCanonicalPath());

		boolean changed = false;
		changed |= prune();
		changed |= search();

		if (changed)
		{
			Services.notifications.localsChanged();
		}
		
		Services.logger.logStream.println("Done synchronizing " + getCanonicalPath());
	}
	
	private LocalDirectory getThis()
	{
		return this;
	}

	public LocalFile getFile(String fsPath)
	{
		return Services.db.findLocalFile(this, new File(fsPath));
	}
	
	public String toString()
	{
		return path /* + " [" + files.size() + " files] [disk usage: " + getFileSize() + "]" */;
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}
}
