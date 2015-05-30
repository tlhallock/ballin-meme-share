package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.PathSecurity;
import org.cnv.shr.sync.ConsecutiveDirectorySyncIterator;
import org.cnv.shr.sync.RemoteFileSource;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.sync.RootSynchronizer;


public class RemoteDirectory extends RootDirectory
{
	PathElement path;
	private SharingState sharesWithUs;
	
	public RemoteDirectory(final Machine machine,
			final String name,
			final String tags, 
			final String description,
			final SharingState defaultShare)
	{
		super(machine, name, tags, description);
		path = DbPaths.getPathElement(
				Services.settings.downloadsDirectory.get().getAbsolutePath() + File.separator
				+ PathSecurity.getFsName(machine.getName()) + File.separator
				+ PathSecurity.getFsName(getName()) + File.separator);
		sharesWithUs = defaultShare;
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
		return new File(path.getFsPath());
	}
	
	public String getLocalMirrorName()
	{
		return "mirror." + getMachine().getIdentifier() + ":" + getName();
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
	public boolean pathIsSecure(final Path canonicalPath)
	{
		return canonicalPath.startsWith(path.getFsPath());
	}

	@Override
	protected void sendNotifications()
	{
		Services.notifications.remoteDirectoryChanged(this);
	}

	@Override
	protected void setSharing(SharingState sharingState)
	{
		this.sharesWithUs = sharingState;
	}
	
	public SharingState getSharesWithUs()
	{
		return sharesWithUs;
	}
	@Override
	protected SharingState getDbSharing()
	{
		return sharesWithUs;
	}
}
