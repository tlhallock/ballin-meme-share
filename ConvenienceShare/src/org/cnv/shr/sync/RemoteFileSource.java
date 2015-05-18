package org.cnv.shr.sync;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.PathList;
import org.cnv.shr.msg.PathList.Child;
import org.cnv.shr.util.FileOutsideOfRootException;

public class RemoteFileSource implements FileSource
{
	private RemoteSynchronizerQueue sync;
	private PathElement pathElement;
	private RemoteFile remoteFile;

	public RemoteFileSource(RemoteDirectory r, RemoteSynchronizerQueue queue) throws UnknownHostException, IOException
	{
		this.sync = queue;
		this.pathElement = DbPaths.ROOT;
	}
	
	private RemoteFileSource(RemoteSynchronizerQueue s, RemoteFile f)
	{
		sync = s;
		remoteFile = f;
		pathElement = f.getPath();
	}
	
	private RemoteFileSource(RemoteSynchronizerQueue s, PathElement p, String name)
	{
		sync = s;
		pathElement = DbPaths.getPathElement(p, name);
	}
	
	RemoteSynchronizerQueue getQueue()
	{
		return sync;
	}
	
	public String toString()
	{
		return pathElement.getFullPath();
	}

	@Override
	public boolean stillExists()
	{
		// We have no way of checking...
		return true;
	}

	@Override
	public FileSourceIterator listFiles() throws IOException
	{
		if (remoteFile != null)
		{
			return FileSource.NULL_ITERATOR;
		}
		
		PathList directoryList = sync.getDirectoryList(pathElement);
		if (directoryList == null)
		{
			return FileSource.NULL_ITERATOR;
		}
		
		final Iterator<String> subDirs = directoryList.getSubDirs().iterator();
		final Iterator<Child> children = directoryList.getChildren().iterator();
		return new FileSourceIterator()
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
				return subDirs.hasNext();
			}

			@Override
			public RemoteFileSource next()
			{
				if (onFiles)
				{
					return new RemoteFileSource(sync, pathElement, subDirs.next());
				}
				else
				{
					return new RemoteFileSource(sync, children.next().create());
				}
			}

			@Override
			public void remove() {}

			@Override
			public void close() throws IOException {}
		};
	}

	@Override
	public String getName()
	{
		return pathElement.getUnbrokenName();
	}

	@Override
	public boolean isDirectory()
	{
		return remoteFile == null;
	}

	@Override
	public boolean isFile()
	{
		return remoteFile != null;
	}

	@Override
	public String getCanonicalPath()
	{
		return pathElement.getFullPath();
	}

	@Override
	public long getFileSize()
	{
		return remoteFile.getFileSize();
	}
	
	@Override
	public SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException
	{
		return remoteFile;
	}
}