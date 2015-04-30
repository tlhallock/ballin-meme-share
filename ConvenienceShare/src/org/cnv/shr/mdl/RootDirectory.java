package org.cnv.shr.mdl;

import java.util.HashMap;

import org.cnv.shr.dmn.Services;

public abstract class RootDirectory
{
	protected Machine machine;
	protected String path;
	protected long totalFileSize;
	protected int totalNumFiles;
	
	public RootDirectory(Machine machine, String path)
	{
		this.machine = machine;
		this.path = path;
	}
	
	public String getPath()
	{
		return path;
	}

	public Machine getMachine()
	{
		return machine;
	}

	public HashMap<String, LocalFile> list()
	{
		return Services.db.list(this);
	}
	
	public SharedFile getFile(String relPath)
	{
		return Services.db.getFile(machine, this, relPath);
	}
	
	public abstract void synchronize();
	public abstract boolean isLocal();
}
