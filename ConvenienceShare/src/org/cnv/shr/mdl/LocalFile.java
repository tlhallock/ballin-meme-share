package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class LocalFile extends SharedFile
{
	public LocalFile(int int1)
	{
		super(int1);
	}

	public LocalFile(LocalDirectory local, PathElement element, String tags, long fileSize, long lastModified, String checksum)
	{
		super(null);

		this.path = element;
		this.rootDirectory = local;
		this.tags = tags;
		this.fileSize = fileSize;
		this.lastModified = lastModified;
		this.checksum = checksum;
	}
	
	public LocalFile(LocalDirectory local, PathElement element) throws IOException
	{
		super(null);
		rootDirectory = local;
		path = element;
		tags = null;
		
		Path f = getFsFile();

		fileSize = Files.size(f);
		lastModified = Files.getLastModifiedTime(f).toMillis();

		Path dir = f.getParent().toRealPath();
		String root = Misc.deSanitize(local.getPathElement().getFullPath());

		if (!dir.startsWith(root))
		{
			throw new FileOutsideOfRootException(dir, root);
		}

		try
		{
			if (shouldChecksum())
			{
				checksum = Services.checksums.checksumBlocking(f, Level.FINE);
			}
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to checksum " + this, ex);
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
	 * @throws IOException 
	 */
	public boolean refreshAndWriteToDb() throws SQLException, IOException
	{
		if (!exists())
		{
			DbFiles.delete(this);
			Services.notifications.fileDeleted(this);
			return true;
		}

		Path fsCopy = Paths.get(getFullPath());
		long fsLastModified = Files.getLastModifiedTime(fsCopy).toMillis();
		if (fsLastModified <= lastModified)
		{
			return false;
		}
		
		lastModified = fsLastModified;
		checksum = null;
		updateChecksum(fsCopy);
		
		fileSize = Files.size(fsCopy);
		tryToSave();
		Services.notifications.fileChanged(this);
		return true;
	}

	private void updateChecksum(Path fsCopy)
	{
		if (!shouldChecksum())
		{
			return;
		}
		try
		{
			checksum = Services.checksums.checksumBlocking(fsCopy, Level.FINE);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to checksum " + fsCopy, e);
		}
	}

	@Override
	public void setChecksum(String checksum) throws IOException
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
				tryToSave();
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write to db", e);
		}
	}
	
	public boolean exists()
	{
		return new File(getFullPath()).exists();
	}

	public Path getFsFile()
	{
		return Paths.get(
				Misc.deSanitize(
					getRootDirectory().getPathElement().getFullPath() 
					+ File.separator
					+ path.getFsPath()));
	}

	public void ensureChecksummed() throws IOException
	{
		if (checksum != null)
		{
			return;
		}
		checksum = Services.checksums.checksumBlocking(getFsFile(), Level.FINE);
		tryToSave();
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	protected LocalDirectory fillRoot(ConnectionWrapper c, DbLocals locals, int rootId)
	{
		return (LocalDirectory) locals.getObject(c, DbTables.DbObjects.LROOT, rootId);
	}
}
