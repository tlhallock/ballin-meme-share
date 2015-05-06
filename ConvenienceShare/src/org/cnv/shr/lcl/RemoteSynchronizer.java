package org.cnv.shr.lcl;

import java.io.IOException;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

public class RemoteSynchronizer extends RootSynchronizer
{
	public RemoteSynchronizer(RemoteDirectory remoteDirectory) throws IOException
	{
		super(remoteDirectory, new FileSource.RemoteFileSource(remoteDirectory));
	}

	@Override
	protected SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void notifyChanged()
	{
		// TODO Auto-generated method stub
		
	}

}
