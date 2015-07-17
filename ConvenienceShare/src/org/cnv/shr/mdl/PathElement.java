
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



package org.cnv.shr.mdl;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.RStringBuilder;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.sync.FileSource;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class PathElement extends DbObject<Long>
{
	protected RootDirectory root;
	protected PathElement parent;
	protected String value;
	protected boolean broken;
	protected long pathId;
	
	String fullPath;
	
	public PathElement(
			Long id,
			RootDirectory root,
			PathElement parent,
			String value,
			boolean broken,
			long pathId)
	{
		super(id);
		this.root = root;
		this.parent = parent;
		this.value = value;
		this.broken = broken;
		this.pathId = pathId;
	}
	
//	public PathElement(Long id)
//	{
//		super(id);
//	}
//	
//	public PathElement(PathElement parent, String value)
//	{
//		super(null);
//		this.parentId = parent;
//		this.value = value;
//		
//		if (value.length() > PathBreaker.PATH_ELEMENT_LENGTH)
//		{
//			throw new RuntimeException("Value is too big: " + value);
//		}
//	}
//
//	public PathElement(PathElement current, long integer, String string)
//	{
//		super(integer);
//		parentId = current;
//		id = integer;
//		value = string;
//		
//		if (value.length() > PathBreaker.PATH_ELEMENT_LENGTH)
//		{
//			throw new RuntimeException("Value is too big: " + value);
//		}
//	}
//
//	public PathElement(PathElement parent, String string, boolean b)
//	{
//		super(null);
//		this.parentId = parent;
//		this.value = string;
//		this.broken = b;
//	}

	public long getPathId()
	{
		return pathId;
	}
	
	@Override
	public String toString()
	{
		return getFullPath();
	}

	public void setId(long i)
	{
		id = i;
	}

	public PathElement getParent()
	{
		return parent;
	}


	@Override
	public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
	{
		id       = row.getLong("P_ID");
		parentId = (PathElement) locals.getObject(c, DbTables.DbObjects.PELEM, row.getInt("PARENT"));
		broken   = row.getBoolean("BROKEN");
		value    = row.getString("PELEM");
	}

	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		throw new RuntimeException("Implement me!");
//		try (StatementWrapper stmt = c.prepareStatement(MERGE1, Statement.RETURN_GENERATED_KEYS);)
//		{
//			int ndx = 1;
//
//			stmt.setLong(ndx++, getParent().getId());
//			stmt.setString(ndx++, value);
//			stmt.setLong(ndx++, getParent().getId());
//			stmt.setBoolean(ndx++, broken);
//			stmt.setString(ndx++, value);
//			stmt.executeUpdate();
//			
//			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
//			{
//				if (generatedKeys.next())
//				{
//					id = generatedKeys.getLong(1);
//					return true;
//				}
//				return false;
//			}
//		}
	}

	public String getName()
	{
		return value;
	}
	
	public String[] getDbValues()
	{
		return new String[] {value};
	}

	public String getUnbrokenName()
	{
		PathElement c = this;
		RStringBuilder builder = new RStringBuilder();
		do
		{
			builder.preppend(c.value);
			c = c.getParent();
		} while (c.isBroken());
		return builder.toString();
	}

	public String getFullPath()
	{
		if (fullPath != null)
		{
			return fullPath;
		}
		PathElement c = this;
		RStringBuilder builder = new RStringBuilder();
		while (c.getParent() != c)
		{
			builder.preppend(c.value);
			c = c.getParent();
		}
		return fullPath = builder.toString();
	}
	
	public String getFsPath()
	{
		return Misc.deSanitize(getFullPath());
	}

	public interface CollectingFilesMonitor
	{
		public boolean continueDownloading();
		public void found();
	}
	
	public void downloadAllCurrentlyCached(RootDirectory root, CollectingFilesMonitor monitor)
	{
		if (!monitor.continueDownloading())
		{
			return;
		}
		SharedFile file = DbFiles.getFile(root, this);
		if (file != null)
		{
			if (file.isLocal())
			{
				return;
			}
			try
			{
				Services.downloads.download(file);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to download " + file, e);
			}
			monitor.found();
			return;
		}
		
		for (PathElement child : list(root))
		{
			if (child.equals(this))
			{
				continue;
			}
			child.downloadAllCurrentlyCached(root, monitor);
		}
	}
	public void getFilesList(RootDirectory root, LinkedList<SharedFile> files)
	{
		SharedFile file = DbFiles.getFile(root, this);
		if (file != null)
		{
			files.add(file);
			return;
		}
		
		for (PathElement child : list(root))
		{
			if (child.equals(this))
			{
				continue;
			}
			child.getFilesList(root, files);
		}
	}
	
	public LinkedList<PathElement> list(RootDirectory local)
	{
		LinkedList<PathElement> returnValue = new LinkedList<>();
		LinkedList<PathElement> brokenQueue = new LinkedList<PathElement>();

		// not as simple as the recursive call, but only opens one statement at a time...
		PathElement current = this;
		while (current != null)
		{
			try (DbIterator<PathElement> iterator = DbPaths.listPathElements(local, current);)
			{
				while (iterator.hasNext())
				{
					PathElement next = iterator.next();
					if (next.isBroken())
					{
						brokenQueue.add(next);
					}
					else
					{
						returnValue.add(next);
					}
				}

				current = brokenQueue.isEmpty() ? null : brokenQueue.removeLast();
			}
		}
		returnValue.sort(PATH_COMPARATOR);
		return returnValue;
	}

	public boolean isBroken()
	{
		return broken;
	}
	
	public static String sanitizeFilename(FileSource f)
	{
		String name = f.getName();
		if (name.length() == 0)
		{
			return name;
		}
		if (f.isDirectory())
		{
			if (name.charAt(name.length() - 1) != '/')
			{
				name = name + "/";
			}
			return name;
		}
		else if (f.isFile())
		{
			return name;
		}
		
		return null;
	}
	
	public boolean equals(PathElement other)
	{
		return id == other.id;
	}

	public PathElement getBrokenBegin()
	{
		PathElement c = this;
		while (c.isBroken())
		{
			c = c.getParent();
		}
		return c;
	}

	public boolean isAbsolute()
	{
		return getUnbrokenName().startsWith("/") || getId() == 0 || Paths.get(getUnbrokenName()).isAbsolute()
				// This probably shouldn't be here...
				|| (Misc.getOperatingSystem().equals(Misc.OperatingSystem.Windows)
						&& getUnbrokenName().length() >= 2 && getUnbrokenName().charAt(1) == ':');
	}

	public static final Comparator<PathElement> PATH_COMPARATOR = new Comparator<PathElement>()
	{
    public int compare(PathElement o1, PathElement o2)
    {
    	String fullPath2 = o1.getFullPath();
			String fullPath3 = o2.getFullPath();
			boolean lIsDir = fullPath2.endsWith("/");
			boolean rIsDir = fullPath3.endsWith("/");
			if (lIsDir && !rIsDir)
			{
				return -1;
			}
			if (rIsDir && !lIsDir)
			{
				return 1;
			}
			return fullPath2.compareTo(fullPath3);
    }
	};

	public RootDirectory getRoot()
	{
		return root;
	}
}
