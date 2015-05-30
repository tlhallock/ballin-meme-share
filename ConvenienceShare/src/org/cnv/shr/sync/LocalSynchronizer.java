package org.cnv.shr.sync;

import java.io.IOException;
import java.sql.SQLException;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.SharedFile;

public class LocalSynchronizer extends RootSynchronizer
{
	public LocalSynchronizer(LocalDirectory rootDirectory, SyncrhonizationTaskIterator iterator) throws IOException
	{
		super(rootDirectory, iterator);
	}

//	protected SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException
//	{
//		return new LocalFile((LocalDirectory) local, element);
//	}

	@Override
	protected boolean updateFile(SharedFile file) throws SQLException, IOException
	{
		return ((LocalFile) file).refreshAndWriteToDb();
	}
}
