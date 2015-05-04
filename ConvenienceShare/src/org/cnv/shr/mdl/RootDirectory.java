package org.cnv.shr.mdl;

import org.cnv.shr.db.h2.DbTables;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;

public abstract class RootDirectory extends DbObject
{
	protected Machine machine;
	protected String path;
	protected long totalFileSize = -1;
	protected long totalNumFiles = -1;
	protected String description;
	protected String tags;
	
	protected RootDirectory(Integer id)
	{
		super(id);
	}

	public RootDirectory(Machine machine2, String path2, String tags2, String description2)
	{
		super(null);
		this.machine = machine2;
		this.path = path2;
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
		
		machine = (Machine)   locals.getObject(c, DbTables.DbObjects.RMACHINE, row.getInt("MID"));
		path = ((PathElement) locals.getObject(c, DbTables.DbObjects.PELEM,    row.getInt("PELEM"))).getFullPath();
	}
	
	public String getCanonicalPath()
	{
		return path;
	}

	public Machine getMachine()
	{
		return machine;
	}
	
	public final void synchronize(boolean force)
	{
		try
		{
			if (!Services.locals.startSynchronizing(this))
			{
				return;
			}
			
			synchronizeInternal();
			totalNumFiles = DbRoots.getNumberOfFiles(this);
			totalFileSize = DbRoots.getTotalFileSize(this);
			update();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			Services.locals.stopSynchronizing(this);
		}
	}
	protected abstract void synchronizeInternal();
	
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

	public void setPath(String path)
	{
		this.path = path;
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
}
