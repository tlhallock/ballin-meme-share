package org.cnv.shr.sync;

import java.io.IOException;
import java.sql.SQLException;

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
		return file.save();
	}
}
