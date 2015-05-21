package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.sync.ConsecutiveDirectorySyncIterator;
import org.cnv.shr.sync.RemoteFileSource;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.sync.RootSynchronizer;


public class RemoteDirectory extends RootDirectory
{
	PathElement path;
	
	public RemoteDirectory(final Machine machine, final String name, final String tags, final String description)
	{
		super(machine, name, tags, description);
		path = DbPaths.getPathElement(Services.settings.downloadsDirectory.get().getAbsolutePath() + "/" + getName());
	}

	public RemoteDirectory(final int int1)
	{
		super(int1);
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
	
	@Override
	public PathElement getPathElement()
	{
		return path;
	}

	@Override
	protected void setPath(final PathElement object)
	{
		this.path = object;
	}

	public File getLocalRoot()
	{
		return new File(path.getFullPath());
	}

	@Override
	protected RootSynchronizer createSynchronizer() throws IOException
	{
		final RemoteSynchronizerQueue createRemoteSynchronizer = Services.syncs.createRemoteSynchronizer(this);
		final RemoteFileSource source = new RemoteFileSource(this, createRemoteSynchronizer);
		final ConsecutiveDirectorySyncIterator consecutiveDirectorySyncIterator = new ConsecutiveDirectorySyncIterator(this, source);
		consecutiveDirectorySyncIterator.setCloseable(createRemoteSynchronizer);
		return new RemoteSynchronizer(this, consecutiveDirectorySyncIterator);
	}
	
	@Override
	public boolean pathIsSecure(final String canonicalPath)
	{
		return true;
	}

	@Override
	protected void sendNotifications()
	{
		Services.notifications.remoteDirectoryChanged(this);
	}
}
