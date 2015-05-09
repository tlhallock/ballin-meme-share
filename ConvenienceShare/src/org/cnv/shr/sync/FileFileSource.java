package org.cnv.shr.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

public class FileFileSource implements FileSource
{
	private Path f;
	
	public FileFileSource(File f)
	{
		this.f = Paths.get(f.getPath());
	}
	FileFileSource(Path f)
	{
		this.f = f;
	}
	
	public String toString()
	{
		return f.toString();
	}

	@Override
	public boolean stillExists()
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
	
	@Override
	public SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException
	{
		return new LocalFile((LocalDirectory) local2, element);
	}
	@Override
	public void close() throws IOException {}
}