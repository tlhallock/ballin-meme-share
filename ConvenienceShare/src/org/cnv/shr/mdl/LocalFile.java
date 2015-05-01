package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;

import org.cnv.shr.dmn.Services;

public class LocalFile extends SharedFile
{
	public LocalFile() {}
	public LocalFile(LocalDirectory local, String lpath)
	{
		lastUpdated = System.currentTimeMillis();
		
		File f = new File(lpath);
		name = f.getName();
		fileSize = f.getTotalSpace();
		
		try
		{
			path = f.getParentFile().getCanonicalPath().substring(
					local.getCanonicalPath().length());
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to get file path: " + f);
			e.printStackTrace(Services.logger.logStream);
		}
		
		rootDirectory = local;
		description = null;
	}
	
	public String getFullPath()
	{
		return rootDirectory.getCanonicalPath() + File.separatorChar + path + File.separatorChar + name;
	}

	/**
	 * @return true if something has changed.
	 */
	public boolean refreshAndWriteToDb()
	{
		long startTime = System.currentTimeMillis();
		
		if (!exists())
		{
			Services.db.removeFile(this);
			return true;
		}

		File fsCopy = new File(getFullPath());
		if (fsCopy.lastModified() < lastUpdated)
		{
			return false;
		}
		
		lastUpdated = startTime;
		updateChecksum(fsCopy);
		
		fileSize = fsCopy.getTotalSpace();
		Services.db.updateFile(this);
		return true;
	}

	private void updateChecksum(File fsCopy)
	{
		if (fileSize > Services.settings.maxImmediateChecksum)
		{
			Services.checksums.checksum(fsCopy);
			return;
		}
		try
		{
			checksum = Services.checksums.checksumBlocking(fsCopy);
			return;
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to checksum " + fsCopy);
			e.printStackTrace(Services.logger.logStream);
		}
		Services.checksums.checksum(fsCopy);
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
			Services.db.updateFile(this);
		}
	}
	
	public boolean exists()
	{
		return new File(getFullPath()).exists();
	}
}
