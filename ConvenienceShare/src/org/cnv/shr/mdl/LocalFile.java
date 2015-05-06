package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.FileOutsideOfRootException;

public class LocalFile extends SharedFile
{
	public LocalFile(int int1)
	{
		super(int1);
	}
	
	public LocalFile(LocalDirectory local, PathElement element) throws IOException
	{
		super(null);
		rootDirectory = local;
		path = element;
		tags = null;
		
		File f = getFsFile();

		fileSize = f.length();
		lastModified = f.lastModified();

		String dir = f.getParentFile().getCanonicalPath();
		String root = local.getCanonicalPath().getFullPath();

		if (!dir.startsWith(root))
		{
			throw new FileOutsideOfRootException(dir, root);
		}

		try
		{
			if (fileSize < Services.settings.maxImmediateChecksum.get())
			{
				checksum = Services.checksums.checksumBlocking(f);
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	public LocalDirectory getRootDirectory()
	{
		return (LocalDirectory) rootDirectory;
	}
	
	public String getFullPath()
	{
		return rootDirectory.getCanonicalPath().getFullPath() + File.separatorChar + path.getFullPath();
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
		save();
		return true;
	}

	private void updateChecksum(File fsCopy)
	{
		if (fileSize > Services.settings.maxImmediateChecksum.get())
		{
			Services.checksums.checksum(this, fsCopy);
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
		Services.checksums.checksum(this, fsCopy);
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
				save();
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

	public File getFsFile()
	{
		return new File(getRootDirectory().getCanonicalPath().getFullPath() + "/" + path.getFullPath());
	}

	public void ensureChecksummed() throws IOException
	{
		if (checksum == null)
		{
			checksum = Services.checksums.checksumBlocking(getFsFile());
		}
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}
}
