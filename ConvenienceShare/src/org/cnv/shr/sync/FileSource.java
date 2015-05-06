package org.cnv.shr.sync;

import java.io.IOException;
import java.util.Iterator;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

public interface FileSource
{
	boolean exists();
	Iterator<FileSource> listFiles() throws IOException;
	String getName();
	boolean isDirectory();
	boolean isFile();
	String getCanonicalPath();
	long getFileSize();
	SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException;
	

	static Iterator<FileSource> NULL_ITERATOR = new Iterator<FileSource>()
	{
		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public RemoteFileSource next()
		{
			return null;
		}

		@Override
		public void remove() {}
	};
}
