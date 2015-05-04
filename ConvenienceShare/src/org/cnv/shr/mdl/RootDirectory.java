package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
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

	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		id                               = row.getInt   ("R_ID"           );
		tags                             = row.getString("TAGS"           );
		description                      = row.getString("DESCR"          );
		totalFileSize                    = row.getLong  ("TSPACE"         );
		totalNumFiles                    = row.getLong  ("NFILES"         );
		
		machine = (Machine) locals.getObject(DbLocals.DbCacheTypes.MACHINE, row.getInt("MID"));
		if (machine == null)
		{
			machine = DbMachines.getMachine(c, row.getInt("MID"  ));
		}
		path = (String) locals.getObject(DbLocals.DbCacheTypes.PATH, row.getInt("PELEM"));
		if (machine == null)
		{
			path = DbPaths.getPath(c,       row.getInt("PELEM"));
		}
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
			totalNumFiles = Services.db.countFiles(this);
			totalFileSize = Services.db.countFileSize(this);
			Services.db.updateDirectory(machine, this);
		}
		finally
		{
			Services.locals.stopSynchronizing(this);
		}
	}
	protected abstract void synchronizeInternal();
	
	public abstract boolean isLocal();

	public int getId()
	{
		if (id == null)
		{
			id = Services.db.getRootId(this);
		}
		return id;
	}

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


	@Override
	public String getTableName()
	{
		return "ROOT";
	}
}
