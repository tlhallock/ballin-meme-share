package org.cnv.shr.mdl;

public class SharedFile
{
	protected String name;
	protected String path;
	protected RootDirectory rootDirectory;
	protected long filesize;
	protected String checksum;
	protected String description;
	protected long lastUpdated;
	
	public String getName()
	{
		return name;
	}

	public String getPath()
	{
		return path;
	}

	public long getSize()
	{
		return filesize;
	}
	
	public String getChecksum()
	{
		return checksum;
	}
}
