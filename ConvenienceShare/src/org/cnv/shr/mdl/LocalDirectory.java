package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.lcl.LocalSynchronizer;
import org.cnv.shr.util.Misc;

public class LocalDirectory extends RootDirectory
{
	public LocalDirectory(File localDirectory) throws IOException
	{
		super(null);
		machine = Services.localMachine;
		path = localDirectory.getCanonicalPath();
		totalFileSize = -1;
		totalNumFiles = -1;
		id = null;
		description = "";
		tags = "";
	}
	
	public LocalDirectory(Integer id)
	{
		super(id);
	}
	
	public void ensureExistsInDb()
	{
		throw new RuntimeException("Implement me!");
	}

	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean contains(String canonicalPath)
	{
		return canonicalPath.startsWith(path);
	}

	@Override
	public void synchronizeInternal()
	{
		new LocalSynchronizer(this).run();
//		/// Needs to be reworked
//		if (!Services.locals.localAlreadyExists(getCanonicalPath()) && !Services.db.addRoot(Services.localMachine, this))
//		{
//			return;
//		}
//
//		Iterator<SharedFile> currentLocals = Services.db.list(this);
//		boolean changed = false;
//		while (currentLocals.hasNext())
//		{
//			changed |= ((LocalFile) currentLocals.next()).refreshAndWriteToDb();
//		}
//	
//		LinkedList<SharedFile> toAdd = new LinkedList<>();
//		Find find = new Find(path);
//		while (find.hasNext())
//		{
//			File f = find.next();
//
//			String absolutePath;
//			try
//			{
//				absolutePath = f.getCanonicalPath();
//			}
//			catch (IOException e)
//			{
//				Services.logger.logStream.println("Unable to get file path: " + f);
//				e.printStackTrace(Services.logger.logStream);
//				continue;
//			}
//
//			if (Services.db.findLocalFile(this, f) != null)
//			{
//				continue;
//			}
//			Services.logger.logStream.println("Found file " + f);
//			try
//			{
//				toAdd.add(new LocalFile(getThis(), absolutePath));
//			}
//			catch(Exception ex)
//			{
//				Services.logger.logStream.println("Skipping file: " + f);
//				ex.printStackTrace(Services.logger.logStream);
//				continue;
//			}
//			changed = true;
//
//			if (toAdd.size() > 50)
//			{
//				Services.db.addFiles(this, toAdd);
//				toAdd.clear();
//
//				totalNumFiles = Services.db.countFiles(this);
//				totalFileSize = Services.db.countFileSize(this);
//				Services.db.updateDirectory(machine, this);
//				Services.notifications.localsChanged();
//			}
//		}
//		Services.db.addFiles(this, toAdd);
//		
//		
//		Services.logger.logStream.println("Synchronizing " + getCanonicalPath());
//
//		boolean changed = false;
//		changed |= prune();
//		changed |= search();
//
//		if (changed)
//		{
//			Services.notifications.localsChanged();
//		}
//		
//		Services.logger.logStream.println("Done synchronizing " + getCanonicalPath());
	}
	
	private LocalDirectory getThis()
	{
		return this;
	}

	public LocalFile getFile(String fsPath)
	{
		return Services.db.findLocalFile(this, new File(fsPath));
	}

	@Override
	public String toString()
	{
		return path + " [number of files: " + Misc.formatNumberOfFiles(totalNumFiles) + "] [disk usage: " + Misc.formatDiskUsage(totalFileSize) + " ]";
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}
}
