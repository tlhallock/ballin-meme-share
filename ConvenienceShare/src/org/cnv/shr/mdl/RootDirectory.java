package org.cnv.shr.mdl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

public abstract class RootDirectory extends DbObject
{
	protected Machine machine;
	protected String name;
	protected long totalFileSize = -1;
	protected long totalNumFiles = -1;
	protected String description;
	protected String tags;
	
	protected RootDirectory(Integer id)
	{
		super(id);
	}

	public RootDirectory(Machine machine2, String name, String tags2, String description2)
	{
		super(null);
		this.machine = machine2;
		this.name = name;
		this.tags = tags2;
		this.description = description2;
	}

	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
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
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
		PreparedStatement stmt;
		int ndx = 1;
		
		if (id == null)
		{
			stmt = c.prepareStatement("merge into ROOT key(PELEM) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
		}
		else
		{
			stmt = c.prepareStatement("merge into ROOT key(R_ID)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(ndx++, getId());
		}
		stmt.setInt(ndx++, getPathElement().getId());
		stmt.setString(ndx++, getTags());
		stmt.setString(ndx++, getDescription());
		stmt.setInt(ndx++, machine.getId());
		stmt.setBoolean(ndx++, isLocal());
		stmt.setLong(ndx++, totalFileSize);
		stmt.setLong(ndx++, totalNumFiles);
		stmt.setString(ndx++, name);
		return stmt;
	}
	
	public abstract PathElement getPathElement();

	public Machine getMachine()
	{
		return machine;
	}
	
	public final void synchronize(List<? extends SynchronizationListener> listeners)
	{
		Services.logger.logStream.println("Synchronizing " + getPathElement().getFullPath());

		try (RootSynchronizer localSynchronizer = createSynchronizer();)
		{
			if (!startSynchronizing(this, localSynchronizer))
			{
				return;
			}
			
			if (listeners != null)
			{
				for (SynchronizationListener listener : listeners)
				{
					localSynchronizer.addListener(listener);
				}
			}
			DebugListener debugListener = new DebugListener(this);
			localSynchronizer.addListener(debugListener);
			
			Timer t = new Timer();
			t.scheduleAtFixedRate(debugListener, DebugListener.DEBUG_REPEAT, DebugListener.DEBUG_REPEAT);
			localSynchronizer.synchronize();
			t.cancel();
			setStats();
			sendNotifications();

			Services.logger.logStream.println("Done synchronizing " + getPathElement().getFullPath());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
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
		catch (SQLException e)
		{
			e.printStackTrace();
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

	public void setMachine(Machine machine)
	{
		this.machine = machine;
	}

	public void setTotalFileSize(long totalFileSize)
	{
		this.totalFileSize = totalFileSize;
	}

	public void setTotalNumFiles(long totalNumFiles)
	{
		this.totalNumFiles = totalNumFiles;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setTags(String tags)
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
	private static synchronized boolean startSynchronizing(RootDirectory d, RootSynchronizer sync)
	{
		RootSynchronizer rootSynchronizer = synchronizing.get(d.getPathElement().getFullPath());
		if (rootSynchronizer == null)
		{
			synchronizing.put(d.getPathElement().getFullPath(), sync);
			return true;
		}
		return false;
	}
	private static synchronized void stopSynchronizing(RootDirectory d)
	{
		RootSynchronizer remove = synchronizing.remove(d.getPathElement().getFullPath());
		if (remove != null)
		{
			remove.quit();
		}
	}
}
