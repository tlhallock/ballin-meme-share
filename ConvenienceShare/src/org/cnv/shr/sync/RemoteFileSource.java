package org.cnv.shr.sync;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.DirectoryList;
import org.cnv.shr.msg.DirectoryList.Child;
import org.cnv.shr.sync.RemoteSynchronizers.RemoteSynchronizerQueue;
import org.cnv.shr.util.FileOutsideOfRootException;

public class RemoteFileSource implements FileSource
{
	private RemoteSynchronizerQueue sync;
	private PathElement e;
	private RemoteFile r;
	private boolean descend;

	public RemoteFileSource(RemoteDirectory r, boolean descend) throws UnknownHostException, IOException
	{
		sync = Services.syncs.createRemoteSynchronizer(r.getMachine(), r);
		this.e = DbPaths.ROOT;
		this.descend = descend;
	}
	private RemoteFileSource(RemoteSynchronizerQueue s, RemoteFile f)
	{
		sync = s;
		r = f;
	}
	private RemoteFileSource(RemoteSynchronizerQueue s, PathElement p, String name)
	{
		sync = s;
		e = DbPaths.getPathElement(p, name);
	}

	@Override
	public boolean exists()
	{
		return false;
	}

	@Override
	public Iterator<FileSource> listFiles() throws IOException
	{
		if (r != null)
		{
			return FileSource.NULL_ITERATOR;
		}
		
		DirectoryList directoryList = sync.getDirectoryList(e);
		if (directoryList == null)
		{
			return FileSource.NULL_ITERATOR;
		}
		
		if (descend)
		{
			for (String subDir : directoryList.getSubDirs())
			{
				sync.queueDirectoryList(DbPaths.getPathElement(e, subDir));
			}
		}
		
		final Iterator<String> subDirs = directoryList.getSubDirs().iterator();
		final Iterator<Child> children = directoryList.getChildren().iterator();
		return new Iterator<FileSource>()
		{
			boolean onFiles;
			
			@Override
			public boolean hasNext()
			{
				if (children.hasNext())
				{
					return true;
				}
				onFiles = true;
				return descend && subDirs.hasNext();
			}

			@Override
			public RemoteFileSource next()
			{
				if (onFiles)
				{
					return new RemoteFileSource(sync, e, subDirs.next());
				}
				else
				{
					return new RemoteFileSource(sync, children.next().create());
				}
			}

			@Override
			public void remove() {}
		};
	}

	@Override
	public String getName()
	{
		return e.getUnbrokenName();
	}

	@Override
	public boolean isDirectory()
	{
		return r == null;
	}

	@Override
	public boolean isFile()
	{
		return r != null;
	}

	@Override
	public String getCanonicalPath()
	{
		return r.getPath().getFullPath();
	}

	@Override
	public long getFileSize()
	{
		return r.getFileSize();
	}
	
	@Override
	public SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException
	{
		return r;
	}
}