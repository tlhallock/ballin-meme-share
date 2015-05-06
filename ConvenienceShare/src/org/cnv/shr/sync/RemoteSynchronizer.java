package org.cnv.shr.sync;

import java.io.IOException;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;

public class RemoteSynchronizer extends RootSynchronizer
{
	public RemoteSynchronizer(RemoteDirectory remoteDirectory) throws IOException
	{
		super(remoteDirectory, new RemoteFileSource(remoteDirectory, true));
	}

	@Override
	protected void notifyChanged()
	{
		Services.notifications.remotesChanged();
	}

	@Override
	protected boolean updateFile(SharedFile file) throws SQLException
	{
		return file.save();
	}
}
