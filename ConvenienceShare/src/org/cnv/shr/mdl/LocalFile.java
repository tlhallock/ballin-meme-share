package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;

import org.cnv.shr.dmn.Services;

public class LocalFile extends SharedFile
{
	public LocalFile() {}
	public LocalFile(LocalDirectory local, String lpath)
	{
		File f = new File(lpath);
		name = f.getName();
		fileSize = f.getTotalSpace();
		lastModified = f.lastModified();
		
		try
		{
			String dir = f.getParentFile().getCanonicalPath();
			String root = local.getCanonicalPath();
			
			if (!dir.startsWith(root))
			{
				throw new RuntimeException("File not inside root! parent=" + dir + " root=" + root);
			}
			if (dir.length() == root.length())
			{
				path = ".";
			}
			else
			{
				path = dir.substring(root.length() + 1);
			}
			if (fileSize < Services.settings.maxImmediateChecksum.get())
			{
				checksum = Services.checksums.checksumBlocking(f);
			}
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
		if (!exists())
		{
			Services.db.removeFile(this);
			return true;
		}

		File fsCopy = new File(getFullPath());
		long fsLastModified = fsCopy.lastModified();
		if (fsLastModified <= lastModified)
		{
			if (checksum == null)
			{
				updateChecksum(fsCopy);
			}
			
			return false;
		}
		
		lastModified = fsLastModified;
		updateChecksum(fsCopy);
		
		fileSize = fsCopy.getTotalSpace();
		Services.db.updateFile(this);
		return true;
	}

	private void updateChecksum(File fsCopy)
	{
		if (fileSize > Services.settings.maxImmediateChecksum.get())
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
