
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class LocalFile extends SharedFile
{
	public LocalFile(int int1)
	{
		super(int1);
	}

	public LocalFile(LocalDirectory local, PathElement element, String tags, long fileSize, long lastModified, String checksum)
	{
		super(null);

		this.path = element;
		this.rootDirectory = local;
		this.tags = tags;
		this.fileSize = fileSize;
		this.lastModified = lastModified;
		this.checksum = checksum;
	}
	
	public LocalFile(LocalDirectory local, PathElement element) throws IOException
	{
		super(null);
		rootDirectory = local;
		path = element;
		tags = null;
		
		Path f = getFsFile();

		fileSize = Files.size(f);
		lastModified = Files.getLastModifiedTime(f).toMillis();

		Path dir = f.getParent().toRealPath();
		String root = Misc.deSanitize(local.getPathElement().getFullPath());

		if (!dir.startsWith(root))
		{
			throw new FileOutsideOfRootException(dir, root);
		}

		try
		{
			if (shouldChecksum())
			{
				checksum = Services.checksums.checksumBlocking(f, Level.FINE);
			}
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to checksum " + this, ex);
		}
	}
	
	
	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		if (!getRootDirectory().contains(getFsFile()))
		{
			return false;
		}
		return super.save(c);
	}

	private boolean shouldChecksum()
	{
		return fileSize < Services.settings.maxImmediateChecksum.get() || false;
	}

	@Override
	public LocalDirectory getRootDirectory()
	{
		return (LocalDirectory) rootDirectory;
	}
	
	/**
	 * @return true if something has changed.
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public boolean refreshAndWriteToDb() throws SQLException, IOException
	{
		if (!exists())
		{
			DbFiles.delete(this);
			Services.notifications.fileDeleted(this);
			return true;
		}

		Path fsCopy = getFsFile();
		long fsLastModified = Files.getLastModifiedTime(fsCopy).toMillis();
		if (fsLastModified <= lastModified)
		{
			return false;
		}
		
		lastModified = fsLastModified;
		checksum = null;
		updateChecksum(fsCopy);
		
		fileSize = Files.size(fsCopy);
		tryToSave();
		Services.notifications.fileChanged(this);
		return true;
	}

	private void updateChecksum(Path fsCopy)
	{
		if (!shouldChecksum())
		{
			return;
		}
		try
		{
			checksum = Services.checksums.checksumBlocking(fsCopy, Level.FINE);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to checksum " + fsCopy, e);
		}
	}

	@Override
	public void setChecksum(String checksum) throws IOException
	{
		if (this.checksum != null && this.checksum.equals(checksum))
		{
			return;
		}
		this.checksum = checksum;
		try
		{
			// save is because refresh doesn't check checksum
			if (!refreshAndWriteToDb())
			{
				tryToSave();
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write to db", e);
		}
	}
	
	public boolean exists()
	{
		return Files.exists(getFsFile());
	}

	public Path getFsFile()
	{
		return Paths.get(
				Misc.deSanitize(
					getRootDirectory().getPathElement().getFullPath() 
					+ File.separator
					+ path.getFsPath()));
	}

	public void ensureChecksummed() throws IOException
	{
		if (checksum != null)
		{
			return;
		}
		checksum = Services.checksums.checksumBlocking(getFsFile(), Level.FINE);
		tryToSave();
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	protected LocalDirectory fillRoot(ConnectionWrapper c, DbLocals locals, int rootId)
	{
		return (LocalDirectory) locals.getObject(c, DbTables.DbObjects.LROOT, rootId);
	}
}
