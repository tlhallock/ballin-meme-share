package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;

public class LocalFile extends SharedFile
{
	public LocalFile(int int1)
	{
		super(int1);
	}
	
	public LocalFile(LocalDirectory local, File f)
	{
		super(null);
		
		name = f.getName();
		fileSize = f.length();
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
				checksum = Services.checksums.checksumBlocking((LocalDirectory) rootDirectory, f);
			}
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to get file path: " + f);
			e.printStackTrace(Services.logger.logStream);
		}
		
		rootDirectory = local;
		tags = null;
	}
	
	public LocalDirectory getRootDirectory()
	{
		return (LocalDirectory) rootDirectory;
	}
	
	public String getFullPath()
	{
		return rootDirectory.getCanonicalPath() + File.separatorChar + path + File.separatorChar + name;
	}

	/**
	 * @return true if something has changed.
	 * @throws SQLException 
	 */
	public boolean refreshAndWriteToDb() throws SQLException
	{
		if (!exists())
		{
			delete();
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
		update();
		return true;
	}

	private void updateChecksum(File fsCopy)
	{
		if (fileSize > Services.settings.maxImmediateChecksum.get())
		{
			Services.checksums.checksum(getRootDirectory(), fsCopy);
			return;
		}
		try
		{
			checksum = Services.checksums.checksumBlocking(getRootDirectory(), fsCopy);
			return;
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to checksum " + fsCopy);
			e.printStackTrace(Services.logger.logStream);
		}
		Services.checksums.checksum(getRootDirectory(), fsCopy);
	}

	public void setChecksum(long timeStamp, String checksum)
	{
		if (checksum != null && checksum.equals(checksum))
		{
			return;
		}
		this.checksum = checksum;
		try
		{
			//
			if (!refreshAndWriteToDb())
			{
				update();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean exists()
	{
		return new File(getFullPath()).exists();
	}
}
