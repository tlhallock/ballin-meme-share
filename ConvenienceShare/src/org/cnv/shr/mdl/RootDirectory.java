package org.cnv.shr.mdl;

import java.util.Iterator;

import org.cnv.shr.dmn.Services;

public abstract class RootDirectory
{
	protected Machine machine;
	protected String path;
	protected long totalFileSize = -1;
	protected long totalNumFiles = -1;
	protected Integer id;
	protected String description;
	protected String tags;
	
	public RootDirectory(Machine machine, String path, String description, String tags)
	{
		this.machine = machine;
		this.path = path;
		this.description = description;
		this.tags = tags;
	}
	
	public String getCanonicalPath()
	{
		return path;
	}

	public Machine getMachine()
	{
		return machine;
	}

	public Iterator<SharedFile> list()
	{
		return Services.db.list(this);
	}
	
	public final void synchronize(boolean force)
	{
		synchronizeInternal();
		totalNumFiles = Services.db.countFiles(this);
		totalFileSize = Services.db.countFileSize(this);
		Services.db.updateDirectory(machine, this);
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
}
