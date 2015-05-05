package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.dmn.Services;
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
		stmt.setInt(ndx++, getCanonicalPath().getId());
		stmt.setString(ndx++, getTags());
		stmt.setString(ndx++, getDescription());
		stmt.setInt(ndx++, machine.getId());
		stmt.setBoolean(ndx++, isLocal());
		stmt.setLong(ndx++, totalFileSize);
		stmt.setLong(ndx++, totalNumFiles);
		stmt.setString(ndx++, name);
		return stmt;
	}
	
	public abstract PathElement getCanonicalPath();

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
			save();
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
}
