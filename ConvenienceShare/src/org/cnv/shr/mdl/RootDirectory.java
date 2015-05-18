package org.cnv.shr.mdl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.sync.DebugListener;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.util.Misc;

public abstract class RootDirectory extends DbObject<Integer>
{
	protected Machine machine;
	protected String name;
	protected long totalFileSize = -1;
	protected long totalNumFiles = -1;
	protected String description;
	protected String tags;
	
	protected RootDirectory(final Integer id)
	{
		super(id);
	}

	public RootDirectory(final Machine machine2, final String name, final String tags2, final String description2)
	{
		super(null);
		this.machine = machine2;
		this.name = name;
		this.tags = tags2;
		this.description = description2;
	}

	@Override
	public void fill(final Connection c, final ResultSet row, final DbLocals locals) throws SQLException
	{
		id                               = row.getInt   ("R_ID"           );
		tags                             = row.getString("TAGS"           );
		description                      = row.getString("DESCR"          );
		totalFileSize                    = row.getLong  ("TSPACE"         );
		totalNumFiles                    = row.getLong  ("NFILES"         );
		name                             = row.getString("RNAME");
		
		machine = (Machine)   locals.getObject(c, DbTables.DbObjects.RMACHINE, row.getInt("MID"));
		setPath((PathElement) locals.getObject(c, DbTables.DbObjects.PELEM,    row.getInt("PELEM")));
	}

	protected abstract void setPath(PathElement object);

	@Override
	public boolean save(final Connection c) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement("merge into ROOT key(R_ID) VALUES ("
				+ "(select R_ID from ROOT where MID=? and RNAME=?)"
				+ ", ?, ?, ?, ?, ?, ?, ?, ?);");)
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
			
			stmt.executeUpdate();
			final ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				id = generatedKeys.getInt(1);
				return true;
			}
			return false;
		}
	}
	
	public abstract PathElement getPathElement();

	public Machine getMachine()
	{
		return machine;
	}
	
	public final void synchronize(final List<? extends SynchronizationListener> listeners)
	{
		Services.logger.println("Synchronizing " + getPathElement().getFullPath());

		try
		{
			final RootSynchronizer localSynchronizer = createSynchronizer();
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
			sendNotifications();

			Services.logger.println("Done synchronizing " + getPathElement().getFullPath());
		}
		catch (final Exception ex)
		{
			Services.logger.print(ex);
		}
		finally
		{
			stopSynchronizing(this);
		}
	}

	public void setStats()
	{
		try
		{
			totalNumFiles = DbRoots.getNumberOfFiles(this);
			totalFileSize = DbRoots.getTotalFileSize(this);
			save();
		}
		catch (final SQLException e)
		{
			Services.logger.print(e);
		}
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

	public abstract boolean pathIsSecure(String canonicalPath);
	protected abstract RootSynchronizer createSynchronizer() throws IOException;
	protected abstract void sendNotifications();
	
	

	public void stopSynchronizing()
	{
		stopSynchronizing(this);
	}
	
	
	

	private static HashMap<String, RootSynchronizer> synchronizing = new HashMap<>();
	private static synchronized boolean startSynchronizing(final RootDirectory d, final RootSynchronizer sync)
	{
		final RootSynchronizer rootSynchronizer = synchronizing.get(d.getPathElement().getFullPath());
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
}
