package org.cnv.shr.mdl;

import java.io.File;

import org.cnv.shr.dmn.Services;

public class LocalFile extends SharedFile
{
	private String absPath;
	private long lastUpdated;
	
	public LocalFile(String path)
	{
		absPath = path;
	}

	public boolean refresh()
	{
		File fsCopy = new File(absPath);
		if (!fsCopy.exists())
		{
			return false;
		}
		if (fsCopy.lastModified() < lastUpdated)
		{
			return true;
		}
		Services.checksums.checksum(fsCopy);
		return true;
	}

	public void setChecksum(String checksum)
	{
		this.checksum = checksum;
	}

	public void setLastUpdated(long startTime)
	{
		lastUpdated = startTime;
	}
}
