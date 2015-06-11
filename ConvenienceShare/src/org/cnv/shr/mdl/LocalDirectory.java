
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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.sync.ConsecutiveDirectorySyncIterator;
import org.cnv.shr.sync.FileFileSource;
import org.cnv.shr.sync.FileSource;
import org.cnv.shr.sync.LocalSynchronizer;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.sync.SyncrhonizationTaskIterator;
import org.cnv.shr.util.Misc;

public class LocalDirectory extends RootDirectory
{
	private static final QueryWrapper UPDATE1 = new QueryWrapper("update ROOT set "
			+ "PELEM=?, TAGS=?, DESCR=?, TSPACE=?, NFILES=?, RNAME=?, SHARING=?, MIN_SIZE=?, MAX_SIZE=? "
			+ "where ROOT.R_ID = ?;");
	
	
	private PathElement path;
	private SharingState defaultShare;
	
	public LocalDirectory(String name,
			String description, 
			String tags, 
			long minFSize,
			long maxFSize, 
			String path2, 
			SharingState defaultSharingState,
			Long totalFileSize,
			Long totalNumFiles)
	{
		super(null);
		this.machine = Services.localMachine;
		this.name = name;
		this.description = description;
		this.tags = tags;
		this.minFSize = minFSize;
		this.maxFSize = maxFSize;
		this.path = DbPaths.getPathElement(path2);
		this.defaultShare = defaultSharingState;
		if (defaultShare == null)
		{
			defaultShare = SharingState.valueOf(Services.settings.defaultPermission.get());
		}
		if (totalFileSize != null)
		this.totalFileSize = totalFileSize;
		if (totalNumFiles != null)
		this.totalNumFiles = totalNumFiles;
	}
	
	public LocalDirectory(PathElement path, String name) throws IOException
	{
		super(null);
		machine = Services.localMachine;
		if (name == null || name.length() == 0)
		{
			// Should check if this local directory already exists...
			this.name = path.getUnbrokenName();
		}
		else
		{
			this.name = name;
		}
		this.path = path;
		totalFileSize = -1;
		totalNumFiles = -1;
		id = null;
		description = "";
		tags = "";
		defaultShare = SharingState.valueOf(Services.settings.defaultPermission.get());
	}
	
	public LocalDirectory(Integer id)
	{
		super(id);
	}

	public void ensureExistsInDb()
	{
		throw new RuntimeException("Implement me!");
	}
	
	@Override
	public boolean pathIsSecure(Path canonicalPath)
	{
		return contains(canonicalPath);
	}

	@Override
	public void setPath(PathElement pathElement)
	{
		path = pathElement;
	}

	public boolean contains(Path toShare)
	{
		return toShare.startsWith(Paths.get(path.getFsPath()));
	}
	
	public LocalFile getFile(String fsPath)
	{
		return DbFiles.getFile(this, DbPaths.getPathElement(this, fsPath));
	}

	@Override
	public String toString()
	{
		return path + " [number of files: " + Misc.formatNumberOfFiles(totalNumFiles) + "] [disk usage: " + Misc.formatDiskUsage(totalFileSize) + " ]";
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public PathElement getPathElement()
	{
		return path;
	}

	@Override
	protected RootSynchronizer createSynchronizer() throws IOException
	{
		File f = new File(getPathElement().getFullPath());
		// This is probably not necessary...
		if (Files.isSymbolicLink(Paths.get(f.getCanonicalPath())) || !f.isDirectory())
		{
			f.delete();
			throw new RuntimeException("Symbolic link: " + f + ". Skipping");
		}
		FileSource source = new FileFileSource(f, DbRoots.getIgnores(this));
		SyncrhonizationTaskIterator iterator = new ConsecutiveDirectorySyncIterator(this, source);
		return new LocalSynchronizer(this, iterator);
	}

	@Override
	public boolean save(final ConnectionWrapper c) throws SQLException
	{
		if (id == null)
		{
			return super.save(c);
		}
		
		try (StatementWrapper stmt = c.prepareStatement(UPDATE1);)
		{
			int ndx = 1;
			
			stmt.setLong(ndx++, getPathElement().getId());
			stmt.setString(ndx++, getTags());
			stmt.setString(ndx++, getDescription());
			stmt.setLong(ndx++, totalFileSize);
			stmt.setLong(ndx++, totalNumFiles);
			stmt.setString(ndx++, getName());
			stmt.setInt(ndx++, getDefaultSharingState().getDbValue());
			stmt.setLong(ndx++, minFSize);
			stmt.setLong(ndx++, maxFSize);
			
			stmt.setInt(ndx++, id);
			
			stmt.executeUpdate();
			try (final ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
					return true;
				}
				return false;
			}
		}
	}

	@Override
	protected void sendNotifications()
	{
		Services.notifications.localChanged(this);
	}

	@Override
	public void setDefaultSharingState(SharingState sharingState)
	{
		this.defaultShare = sharingState;
	}
	
	public SharingState getDefaultSharingState()
	{
		return defaultShare;
	}

	@Override
	protected SharingState getDbSharing()
	{
		return defaultShare;
	}

	public void setName(String text)
	{
		this.name = text;
	}
}
