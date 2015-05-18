package org.cnv.shr.sync;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

public interface FileSource
{
	boolean stillExists();
	FileSourceIterator listFiles() throws IOException;
	String getName();
	boolean isDirectory();
	boolean isFile();
	String getCanonicalPath();
	long getFileSize();
	SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException;
	

	static FileSourceIterator NULL_ITERATOR = new FileSourceIterator()
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

		@Override
		public void close() throws IOException {}
	};
	
	interface FileSourceIterator extends Iterator<FileSource>, Closeable {}
}
