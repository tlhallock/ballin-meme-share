package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.dwn.ChecksumRequest;

public class DownloadManager
{
	private HashMap<SharedFileId, DownloadInstance> downloads = new HashMap<>();

	public DownloadInstance download(SharedFile remoteFile) throws UnknownHostException, IOException
	{
		if (remoteFile.isLocal())
		{
			Services.logger.println("Trying to download local file " + remoteFile);
			return null;
		}
		return download((RemoteFile) remoteFile);
	}
	
	public DownloadInstance download(RemoteFile file) throws UnknownHostException, IOException
	{
		DownloadInstance download = createDownload(new Download(file));
		download.begin();
		return download;
	}
	
	public synchronized DownloadInstance createDownload(Download d) throws UnknownHostException, IOException
	{
		DownloadInstance prev = downloads.get(d.getFile().getId());
		if (prev != null)
		{
			return prev;
		}
		
		try
		{
			d.save(Services.h2DbCache.getConnection());
		}
		catch (SQLException e)
		{
			Services.logger.println("Unable to write new download to database.");
			e.printStackTrace();
		}
		
		String checksum = d.getFile().getChecksum();
		if (checksum == null)
		{
			requestChecksum(d.getFile());
			return null;
		}
		
		DownloadInstance instance = new DownloadInstance(d);
		Services.notifications.downloadAdded(instance);
		downloads.put(new SharedFileId(d.getFile()), instance);
		return instance;
	}

	public synchronized void remove(DownloadInstance downloadInstance)
	{
		downloads.remove(new SharedFileId(downloadInstance.getDownload().getFile()));
	}
	
	public synchronized LinkedList<DownloadInstance> getDownloadInstances(Communication c)
	{
		LinkedList<DownloadInstance> returnValue = new LinkedList<>();
		for (DownloadInstance instance : downloads.values())
		{
			if (instance.contains(c))
			{
				returnValue.add(instance);
			}
		}
		return returnValue;
	}
	public synchronized DownloadInstance getDownloadInstance(SharedFileId descriptor)
	{
		return downloads.get(descriptor);
	}
	
	public void initiatePendingDownloads()
	{
		try
		{
			Connection connection = Services.h2DbCache.getConnection();
			DbIterator<Download> dbIterator = new DbIterator<Download>(connection, connection.prepareStatement(
					"select * from Download order by PRIORITY").executeQuery(),
					DbObjects.PENDING_DOWNLOAD);
			while (dbIterator.hasNext())
			{
				final Download next = dbIterator.next();
				Services.userThreads.execute(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							createDownload(next).begin();
						}
						catch (IOException e)
						{
							Services.logger.print(e);
						}
					}
				});
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
	
	public void quitAllDownloads()
	{
		for (DownloadInstance instance : downloads.values())
		{
			instance.fail("Closing all downloads.");
		}
	}
	
	
	
	public void requestChecksum(RemoteFile remote)
	{
		try
		{
			Communication openConnection = Services.networkManager.openConnection(remote.getRootDirectory().getMachine(), false);
			if (openConnection != null)
			{
				openConnection.send(new ChecksumRequest(remote));
			}
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}
}
