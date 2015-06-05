package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
				Services.settings.downloadsDirectory.get().getAbsolutePath() + File.separator + getLocalMirrorName());
		sharesWithUs = defaultShare;
	}

	public RemoteDirectory(final int int1)
	{
		super(int1);
		// just for now...   
		DbPaths.pathLiesIn(DbPaths.ROOT, this);
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

	public Path getLocalRoot()
	{
		return Paths.get(path.getFsPath());
	}
	
	public String getLocalMirrorName()
	{
		String string = "mirror" + "." + PathSecurity.getFsName(getName()) + "." + PathSecurity.getFsName(getMachine().getIdentifier());
		if (string.length() > MAX_DIRECTORY_NAME_LENGTH)
		{
			string = string.substring(0, MAX_DIRECTORY_NAME_LENGTH);
		}
		return string;
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
	protected void setDefaultSharingState(SharingState sharingState)
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
