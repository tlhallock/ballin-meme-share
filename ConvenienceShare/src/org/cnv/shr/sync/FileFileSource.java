package org.cnv.shr.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbRoots.IgnorePatterns;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;

public class FileFileSource implements FileSource
{
	private Path f;
	IgnorePatterns patterns;
	
	public FileFileSource(File f, IgnorePatterns ignores)
	{
		this.f = Paths.get(f.getPath());
		this.patterns = ignores;
	}
	
	FileFileSource(Path f, IgnorePatterns ignores)
	{
		this.f = f;
		this.patterns = ignores;
	}
	
	@Override
	public String toString()
	{
		return f.toString();
	}

	@Override
	public boolean stillExists()
	{
		return f.toFile().exists() && !patterns.blocks(f.toFile().getAbsolutePath());
	}

	@Override
	public FileSourceIterator listFiles() throws IOException
	{
		return new FileSourceIterator()
		{
			private DirectoryStream<Path> stream = Files.newDirectoryStream(f);
			
			Iterator<Path> it = stream.iterator();
			Path next = findNext();

			@Override
			public boolean hasNext()
			{
				return next != null;
			}

			@Override
			public FileSource next()
			{
				FileSource returnValue = new FileFileSource(next, patterns);
				next = findNext();
				return returnValue;
			}

			@Override
			public void remove()
			{
				it.remove();
			}

			@Override
			public void close() throws IOException
			{
				stream.close();
			}
			
			protected Path findNext()
			{
				while (it.hasNext())
				{
					Path maybeNext = it.next();
					if (!patterns.blocks(maybeNext.toFile().getAbsolutePath()))
					{
						return maybeNext;
					}
				}
				return null;
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to get canonical path:" + f, e);
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
}