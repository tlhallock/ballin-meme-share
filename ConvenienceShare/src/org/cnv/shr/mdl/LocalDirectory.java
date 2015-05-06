package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.util.Timer;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.lcl.FileSource;
import org.cnv.shr.lcl.RootSynchronizer;
import org.cnv.shr.lcl.FileSource.FileFileSource;
import org.cnv.shr.lcl.LocalSynchronizer;
import org.cnv.shr.util.Misc;

public class LocalDirectory extends RootDirectory
{
	private PathElement path;
	
	public LocalDirectory(PathElement path) throws IOException
	{
		super(null);
		machine = Services.localMachine;
		name = path.getName();
		this.path = path;
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

	public void setPath(PathElement pathElement)
	{
		path = pathElement;
	}

	public boolean contains(String canonicalPath)
	{
		return canonicalPath.startsWith(path.getFullPath());
	}

	@Override
	protected void synchronizeInternal() throws IOException
	{
		Timer t = new Timer();
		RootSynchronizer localSynchronizer = new LocalSynchronizer(this);
		t.scheduleAtFixedRate(localSynchronizer, RootSynchronizer.DEBUG_REPEAT, RootSynchronizer.DEBUG_REPEAT);
		localSynchronizer.synchronize();
		t.cancel();
	}
	
	public LocalFile getFile(String fsPath)
	{
		return DbFiles.getFile(this, DbPaths.getPathElement(this, fsPath));
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

	@Override
	public PathElement getCanonicalPath()
	{
		return path;
	}
}
