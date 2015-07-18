
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



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
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;

public class FileFileSource implements FileSource
{
	private Path f;
	private IgnorePatterns patterns;
	
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
		return f.toFile().exists() && !patterns.blocks(f);
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
					if (patterns.blocks(maybeNext))
					{
						continue;
					}
					if (Files.isSymbolicLink(maybeNext))
					{
						continue;
					}
					return maybeNext;
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
	public SharedFile create(PathElement element) throws IOException, FileOutsideOfRootException
	{
		return new LocalFile(element);
	}
}
