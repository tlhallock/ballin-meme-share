package org.cnv.shr.mdl;

import org.cnv.shr.dmn.Services;

public class SharedFile
{
	protected Integer id;
	
	protected String name;
	protected String path;
	protected RootDirectory rootDirectory;
	protected long fileSize;
	protected String checksum;
	protected String description;
	protected long lastModified;
	
	
	public int getId()
	{
		if (id == null)
		{
			id = Services.db.getFile(rootDirectory, path, name).id;
		}
		return id;
	}

	public void setId(int int1)
	{
		this.id = int1;
	}
	
	public String getName()
	{
		return name;
	}

	public String getRelativePath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public RootDirectory getRootDirectory()
	{
		return rootDirectory;
	}

	public void setRootDirectory(RootDirectory rootDirectory)
	{
		this.rootDirectory = rootDirectory;
	}

	public long getFileSize()
	{
		return fileSize;
	}

	public void setFileSize(long filesize)
	{
		this.fileSize = filesize;
	}

	public String getChecksum()
	{
		return checksum;
	}

	public void setChecksum(String checksum)
	{
		this.checksum = checksum;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public long getLastUpdated()
	{
		return lastModified;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setLastUpdated(long long1)
	{
		this.lastModified = long1;
	}
}
