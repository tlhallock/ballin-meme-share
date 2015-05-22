package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbFiles;
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
		String root = local.getPathElement().getFullPath();

		if (!dir.startsWith(root))
		{
			throw new FileOutsideOfRootException(dir, root);
		}

		try
		{
			if (shouldChecksum())
			{
				checksum = Services.checksums.checksumBlocking(f);
			}
		}
		catch (IOException ex)
		{
			Services.logger.print(ex);
		}
	}

	private boolean shouldChecksum()
	{
		return fileSize < Services.settings.maxImmediateChecksum.get() || false;
	}

	@Override
	public LocalDirectory getRootDirectory()
	{
		return (LocalDirectory) rootDirectory;
	}
	
	public String getFullPath()
	{
		return rootDirectory.getPathElement().getFullPath() + File.separatorChar + path.getFullPath();
	}

	/**
	 * @return true if something has changed.
	 * @throws SQLException 
	 */
	public boolean refreshAndWriteToDb() throws SQLException
	{
		if (!exists())
		{
			DbFiles.delete(this);
			Services.notifications.fileDeleted(this);
			return true;
		}

		File fsCopy = new File(getFullPath());
		long fsLastModified = fsCopy.lastModified();
		if (fsLastModified <= lastModified)
		{
			return false;
		}
		
		lastModified = fsLastModified;
		checksum = null;
		updateChecksum(fsCopy);
		
		fileSize = fsCopy.getTotalSpace();
		save();
		Services.notifications.fileChanged(this);
		return true;
	}

	private void updateChecksum(File fsCopy)
	{
		if (!shouldChecksum())
		{
			return;
		}
		try
		{
			checksum = Services.checksums.checksumBlocking(fsCopy);
		}
		catch (IOException e)
		{
			Services.logger.println("Unable to checksum " + fsCopy);
			Services.logger.print(e);
		}
	}

	@Override
	public void setChecksum(String checksum)
	{
		if (checksum != null && checksum.equals(checksum))
		{
			return;
		}
		this.checksum = checksum;
		try
		{
			// save is because refresh doesn't check checksum
			if (!refreshAndWriteToDb())
			{
				save();
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
	
	public boolean exists()
	{
		return new File(getFullPath()).exists();
	}

	public File getFsFile()
	{
		return new File(getRootDirectory().getPathElement().getFullPath() + "/" + path.getFullPath());
	}

	public void ensureChecksummed() throws IOException
	{
		if (checksum == null)
		{
			checksum = Services.checksums.checksumBlocking(getFsFile());
			try
			{
				save();
			}
			catch (SQLException e)
			{
				Services.logger.print(e);
			}
		}
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}
}
