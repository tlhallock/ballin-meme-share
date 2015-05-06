package org.cnv.shr.lcl;

import java.io.File;
import java.io.IOException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.lcl.FileSource.FileFileSource;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

public class LocalSynchronizer extends RootSynchronizer
{
	public LocalSynchronizer(LocalDirectory remoteDirectory) throws IOException
	{
		super(remoteDirectory, new FileFileSource(new File(remoteDirectory.getCanonicalPath().getFullPath())));
	}

	protected SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException
	{
		return new LocalFile((LocalDirectory) local, element);
	}

	protected void notifyChanged()
	{
		Services.notifications.localChanged((LocalDirectory) local);
	}
}
