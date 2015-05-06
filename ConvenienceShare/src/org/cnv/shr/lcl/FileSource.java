package org.cnv.shr.lcl;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.lcl.RemoteSynchronizers.RemoteSynchronizer;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.DirectoryList;
import org.cnv.shr.msg.DirectoryList.Child;

public interface FileSource
{
	boolean exists();
	Iterator<FileSource> listFiles() throws IOException;

	public class FileFileSource implements FileSource
	{
		Path f;
		
		public FileFileSource(File f)
		{
			this.f = Paths.get(f.getPath());
		}
		FileFileSource(Path f)
		{
			this.f = f;
		}

		@Override
		public boolean exists()
		{
			return f.toFile().exists();
		}

		@Override
		public Iterator<FileSource> listFiles() throws IOException
		{
			return new Iterator<FileSource>()
			{
					Iterator<Path> it = Files.newDirectoryStream(f).iterator();

					@Override
					public boolean hasNext()
					{
						return it.hasNext();
					}

					@Override
					public FileSource next()
					{
						return new FileFileSource(it.next());
					}

					@Override
					public void remove()
					{
						it.remove();
					}
			};
		}
		@Override
		public String getName()
		{
			return f.toFile().getName();
		}
		@Override
		public boolean isDirectory()
		{
			return f.toFile().isDirectory();
		}
		@Override
		public boolean isFile()
		{
			return f.toFile().isFile();
		}
		@Override
		public String getCanonicalPath()
		{
			try
			{
				return f.toFile().getCanonicalPath();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return f.toFile().getAbsolutePath();
			}
		}
		@Override
		public long getFileSize()
		{
			return f.toFile().length();
		}
	}
	
	public class RemoteFileSource implements FileSource
	{
		private RemoteSynchronizer sync;
		private PathElement e;
		private RemoteFile r;

		public RemoteFileSource(RemoteDirectory r) throws UnknownHostException, IOException
		{
			sync = Services.syncs.createRemoteSynchronizer(r.getMachine(), r);
			this.e = DbPaths.ROOT;
		}
		private RemoteFileSource(RemoteSynchronizer s, RemoteFile f)
		{
			sync = s;
			r = f;
		}
		private RemoteFileSource(RemoteSynchronizer s, PathElement p, String name)
		{
			sync = s;
			e = DbPaths.createPathElement(p, name);
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
				return new Iterator<FileSource>() {

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
					public void remove() {}};
				
			}
			DirectoryList directoryList = sync.getDirectoryList(e.getFullPath());
			for (String subDir : directoryList.getSubDirs())
			{
				sync.queueDirectoryList(sync.root, DbPaths.createPathElement(e, subDir));
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
					return subDirs.hasNext();
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
	}

	String getName();
	boolean isDirectory();
	boolean isFile();
	String getCanonicalPath();
	long getFileSize();
	
//	public class RemoteFileSource implements FileSource
//	{
//		
//	}
}
