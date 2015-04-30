package org.cnv.shr.mdl;

import java.io.File;

import org.cnv.shr.dmn.Services;

public class LocalFile extends SharedFile
{
	public LocalFile(LocalDirectory local, String path)
	{
		lastUpdated = System.currentTimeMillis();
		
		File f = new File(path);
		name = f.getName();
		filesize = f.getTotalSpace();
		
		path = f.getParentFile().getAbsolutePath().substring(
				local.getPath().length());
		
		rootDirectory = local;
		description = null;
	}
	
	public String getFullPath()
	{
		return rootDirectory.getPath() + File.separatorChar + path + File.separatorChar + name;
	}

	/**
	 * @return true if something has changed.
	 */
	public boolean refreshAndWriteToDb()
	{
		long startTime = System.currentTimeMillis();
		
		if (!exists())
		{
			removeFromDb();
			return true;
		}

		File fsCopy = new File(getFullPath());
		if (fsCopy.lastModified() < lastUpdated)
		{
			return false;
		}
		
		lastUpdated = startTime;
		// update info...
		Services.checksums.checksum(fsCopy);
		filesize = fsCopy.getTotalSpace();
		
		writeToDb();
		return true;
	}

	public void setChecksum(long timeStamp, String checksum)
	{
		if (checksum != null && checksum.equals(checksum))
		{
			return;
		}
		this.checksum = checksum;
		if (!refreshAndWriteToDb())
		{
			writeToDb();
		}
	}
	
	public boolean exists()
	{
		return new File(getFullPath()).exists();
	}
	
	private void writeToDb()
	{
		Services.db.updateFile(this);
	}
	private void removeFromDb()
	{
		Services.db.removeFile(this);
	}
}
