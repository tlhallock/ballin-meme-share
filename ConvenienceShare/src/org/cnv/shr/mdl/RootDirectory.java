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
	
	public RootDirectory(Machine machine, String path)
	{
		this.machine = machine;
		this.path = path;
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
//	
//	public SharedFile getFile(String relPath)
//	{
//		return Services.db.getFile(machine, this, relPath);
//	}
	
	public final void synchronize()
	{
		synchronizeInternal();
		totalNumFiles = Services.db.countFiles(this);
		totalFileSize = Services.db.countFileSize(this);
		
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
}
