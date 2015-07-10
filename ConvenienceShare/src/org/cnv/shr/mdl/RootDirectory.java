
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
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;

import javax.swing.JFrame;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.sync.DebugListener;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public abstract class RootDirectory extends DbObject<Integer>
{
	public static final int MAX_DIRECTORY_NAME_LENGTH = 256;
	
	private static final QueryWrapper MERGE1 = new QueryWrapper("merge into ROOT key(R_ID) VALUES ("
			+ "(select R_ID from ROOT where MID=? and RNAME=?)"
			+ ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
	
	protected Machine machine;
	protected String name;
	protected long totalFileSize = -1;
	protected long totalNumFiles = -1;
	protected String description;
	protected String tags;
	protected long minFSize = -1;
	protected long maxFSize = -1;

	protected RootDirectory(final Integer id)
	{
		super(id);
	}

	public RootDirectory(final Machine machine2, final String name, final String tags2, final String description2)
	{
		super(null);
		this.machine = machine2;
		this.name = name;
		if (this.name.length() > MAX_DIRECTORY_NAME_LENGTH)
		{
			this.name = name.substring(0, MAX_DIRECTORY_NAME_LENGTH);
		}
		this.tags = tags2;
		this.description = description2;
	}

	@Override
	public void fill(final ConnectionWrapper c, final ResultSet row, final DbLocals locals) throws SQLException
	{
		id                               = row.getInt   ("R_ID"           );
		tags                             = row.getString("TAGS"           );
		description                      = row.getString("DESCR"          );
		totalFileSize                    = row.getLong  ("TSPACE"         );
		totalNumFiles                    = row.getLong  ("NFILES"         );
		name                             = row.getString("RNAME");
		minFSize                         = row.getLong  ("MIN_SIZE");
		maxFSize                         = row.getLong  ("MAX_SIZE");
		setDefaultSharingState(       SharingState.get(row.getInt(   "SHARING")));
		
		machine = (Machine)   locals.getObject(c, DbTables.DbObjects.RMACHINE, row.getInt("MID"));
		setPath((PathElement) locals.getObject(c, DbTables.DbObjects.PELEM,    row.getInt("PELEM")));
	}

	protected abstract void setDefaultSharingState(SharingState sharingState);
	protected abstract void setPath(PathElement object);

	@Override
	public boolean save(final ConnectionWrapper c) throws SQLException
	{
		if (this.name.length() > MAX_DIRECTORY_NAME_LENGTH)
		{
			this.name = name.substring(0, MAX_DIRECTORY_NAME_LENGTH);
		}
		try (StatementWrapper stmt = c.prepareStatement(MERGE1);)
		{
			int ndx = 1;
			stmt.setInt(ndx++, getMachine().getId());
			stmt.setString(ndx++, getName());

			stmt.setLong(ndx++, getPathElement().getId());
			stmt.setString(ndx++, getTags());
			stmt.setString(ndx++, getDescription());
			stmt.setInt(ndx++, getMachine().getId());
			stmt.setBoolean(ndx++, isLocal());
			stmt.setLong(ndx++, totalFileSize);
			stmt.setLong(ndx++, totalNumFiles);
			stmt.setString(ndx++, getName());
			stmt.setInt(ndx++, getDbSharing().getDbValue());
			stmt.setLong(ndx++, minFSize);
			stmt.setLong(ndx++, maxFSize);
			
			stmt.executeUpdate();
			try (final ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
					DbPaths.pathLiesIn(getPathElement(), this);
					return true;
				}
				if (id != null)
				{
					DbPaths.pathLiesIn(getPathElement(), this);
				}
				return false;
			}
		}
	}
	
	@Override
	public String toString()
	{
		return machine.getName() + ":" + getPathElement().getFullPath();
	}
	
	protected abstract SharingState getDbSharing();

	public abstract PathElement getPathElement();

	public Machine getMachine()
	{
		return machine;
	}
	
	public final void synchronize(JFrame origin, final List<? extends SynchronizationListener> listeners)
	{
		LogWrapper.getLogger().info("Synchronizing " + getPathElement().getFullPath());

		try (RootSynchronizer localSynchronizer = createSynchronizer(origin))
		{
			if (!startSynchronizing(this, localSynchronizer))
			{
				return;
			}
			
			if (listeners != null)
			{
				for (final SynchronizationListener listener : listeners)
				{
					localSynchronizer.addListener(listener);
				}
			}
			final DebugListener debugListener = new DebugListener(this);
			localSynchronizer.addListener(debugListener);
			
			final Timer t = new Timer();
			t.scheduleAtFixedRate(debugListener, DebugListener.DEBUG_REPEAT, DebugListener.DEBUG_REPEAT);
			localSynchronizer.run();
			t.cancel();
			setStats();

			LogWrapper.getLogger().info("Done synchronizing " + getPathElement().getFullPath());
		}
		catch (final Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to synchronize " + this, ex);
		}
		finally
		{
			stopSynchronizing(this);
			sendNotifications();
		}
	}

	public void setStats()
	{
		totalNumFiles = DbRoots.getNumberOfFiles(this);
		totalFileSize = DbRoots.getTotalFileSize(this);
		tryToSave();
	}

	public abstract boolean isLocal();

	public long numFiles()
	{
		return totalNumFiles;
	}

	public long diskSpace()
	{
		return totalFileSize;
	}

	public String getTags()
	{
		return tags;
	}
	
	public String getDescription()
	{
		return description;
	}

	public void setMachine(final Machine machine)
	{
		this.machine = machine;
	}

	public void setTotalFileSize(final long totalFileSize)
	{
		this.totalFileSize = totalFileSize;
	}

	public void setTotalNumFiles(final long totalNumFiles)
	{
		this.totalNumFiles = totalNumFiles;
	}

	public void setId(final Integer id)
	{
		this.id = id;
	}

	public void setDescription(final String description)
	{
		this.description = description;
	}

	public void setTags(final String tags)
	{
		this.tags = tags;
	}
	
	public String getTotalFileSize()
	{
		return Misc.formatDiskUsage(totalFileSize);
	}
	
	public String getTotalNumberOfFiles()
	{
		return Misc.formatNumberOfFiles(totalNumFiles);
	}

	public String getName()
	{
		return name;
	}

	public abstract boolean pathIsSecure(Path canonicalPath);
	protected abstract RootSynchronizer createSynchronizer(JFrame origin) throws IOException, InterruptedException;
	protected abstract void sendNotifications();
	
	

	public void stopSynchronizing()
	{
		stopSynchronizing(this);
	}
	
	
	

	private static HashMap<String, RootSynchronizer> synchronizing = new HashMap<>();
	private static synchronized boolean startSynchronizing(final RootDirectory d, final RootSynchronizer sync)
	{
		final RootSynchronizer rootSynchronizer =
				synchronizing.get(d.getPathElement().getFullPath());
		if (rootSynchronizer == null)
		{
			synchronizing.put(d.getPathElement().getFullPath(), sync);
			return true;
		}
		return false;
	}
	
	private static synchronized void stopSynchronizing(final RootDirectory d)
	{
		final RootSynchronizer remove = synchronizing.remove(d.getPathElement().getFullPath());
		if (remove != null)
		{
			remove.quit();
		}
	}
	
	private static synchronized boolean isSynchronizing(RootDirectory localDirectory)
	{
		return synchronizing.containsKey(localDirectory.getPathElement().getFullPath());
	}
	
	public boolean isSynchronizing()
	{
		return isSynchronizing(this);
	}

	public long getMinFileSize()
	{
		return minFSize;
	}

	public long getMaxFileSize()
	{
		return maxFSize;
	}

	public void setMinimumSize(long minimumSize)
	{
		minFSize = minimumSize;
	}
}
