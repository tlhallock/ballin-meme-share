
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


package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Level;

public class KeyedFile
{
	private String rootPath;
	
	public KeyedFile(File rootDirectory) throws IOException
	{
		rootDirectory.mkdirs();
		rootPath = rootDirectory.getCanonicalPath();
	}
	
	public void put(String key, String value) throws FileNotFoundException, IOException
	{
		try (PrintStream output = new PrintStream(new FileOutputStream(getFile(key))))
		{
			output.print(value);
		}
	}
	
	public String get(String key) throws IOException
	{
		return get(getFile(key));
	}
	
	private String get(File f) throws IOException
	{
		try (BufferedReader input = new BufferedReader(new FileReader(f)))
		{
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = input.readLine()) != null)
			{
				builder.append(line).append('\n');
			}
			return builder.toString();
		}
		catch (FileNotFoundException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get file " + f, e);
			return null;
		}
	}
	
	public void remove(String key)
	{
		getFile(key).delete();
	}
	
	public void delete() throws IOException
	{
		final DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(Paths.get(rootPath));
		for (Path p : newDirectoryStream)
		{
			p.toFile().delete();
		}
		newDirectoryStream.close();
		new File(rootPath).delete();
	}
	
	public Iterator<Entry> entries() throws IOException
	{
		final DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(Paths.get(rootPath));
		final Iterator<Path> directoryStream = newDirectoryStream.iterator();
		return new Iterator<Entry>()
		{
			private Entry next = getNext();
			
			@Override
			public boolean hasNext()
			{
				return next != null;
			}

			@Override
			public Entry next()
			{
				Entry old = next;
				next = getNext();
				return old;
			}
			
			Entry getNext()
			{
				while (directoryStream.hasNext())
				{
					File f = directoryStream.next().toFile();
					try
					{
						return new Entry(f.getName(), get(f));
					}
					catch (IOException e)
					{
						LogWrapper.getLogger().log(Level.INFO, "Unable to create entry from " + f, e);
					}
				}
				return null;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Delete not supported!!");
			}
			
			@Override
			public void finalize()
			{
				try
				{
					newDirectoryStream.close();
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close directory stream in finalize", e);
				}
			}
		};
	}
	
	public class Entry
	{
		public final String key;
		public final String value;
		
		public Entry(String name, String string)
		{
			this.key = name;
			this.value = string;
		}
	}
	
	private File getFile(String key)
	{
		return new File(key + File.separator + key);
	}
}
