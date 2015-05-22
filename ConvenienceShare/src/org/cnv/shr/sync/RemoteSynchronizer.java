package org.cnv.shr.sync;

import java.io.IOException;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;

public class RemoteSynchronizer extends RootSynchronizer
{
	public RemoteSynchronizer(RemoteDirectory remoteDirectory, SyncrhonizationTaskIterator iterator) throws IOException
	{
		super(remoteDirectory, iterator);
	}

	@Override
	protected boolean updateFile(SharedFile file) throws SQLException
	{
		boolean returValue = file.save();
		if (returValue)
		{
			Services.notifications.fileChanged(file);
		}
		return returValue;
	}
}
