package org.cnv.shr.mdl;

import java.io.IOException;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.sync.LocalSynchronizer;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.util.Misc;

public class LocalDirectory extends RootDirectory
{
	private PathElement path;
	
	public LocalDirectory(PathElement path) throws IOException
	{
		super(null);
		machine = Services.localMachine;
		name = path.getUnbrokenName();
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
	
	public boolean pathIsSecure(String canonicalPath)
	{
		return contains(canonicalPath);
	}

	public void setPath(PathElement pathElement)
	{
		path = pathElement;
	}

	public boolean contains(String canonicalPath)
	{
		return canonicalPath.startsWith(path.getFullPath());
	}
	
	public LocalFile getFile(String fsPath)
	{
		return (LocalFile) DbFiles.getFile(this, DbPaths.getPathElement(this, fsPath));
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
	public PathElement getPathElement()
	{
		return path;
	}

	@Override
	protected RootSynchronizer createSynchronizer() throws IOException
	{
		return new LocalSynchronizer(this);
	}

	@Override
	protected void sendNotifications()
	{
		Services.notifications.localChanged(this);
	}
}
