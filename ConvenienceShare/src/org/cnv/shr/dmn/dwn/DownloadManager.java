package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
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
			JOptionPane.showMessageDialog(Services.application,
					"Unable to download local file: " + remoteFile.getRootDirectory().getPathElement().getFullPath() + ":" + remoteFile.getPath().getFullPath(),
					"Unable to download local file.",
					JOptionPane.INFORMATION_MESSAGE);
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

	private synchronized DownloadInstance createDownload(Download d) throws UnknownHostException, IOException
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

		if (downloads.size() >= Services.settings.maxDownloads.get())
		{
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
		if (downloads.size() >= Services.settings.maxDownloads.get())
		{
			return;
		}
		initiatePendingDownloads();
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

	public synchronized DownloadInstance getDownloadInstanceForGui(SharedFileId descriptor)
	{
		DownloadInstance downloadInstance = downloads.get(descriptor);
		if (downloadInstance == null)
		{
			return null;
		}
		return downloadInstance;
	}
	public synchronized DownloadInstance getDownloadInstance(SharedFileId descriptor, Communication connection)
	{
		DownloadInstance downloadInstance = downloads.get(descriptor);
		if (downloadInstance == null)
		{
			return null;
		}
		if (!downloadInstance.contains(connection))
		{
			return null;
		}
		return downloadInstance;
	}

	public void initiatePendingDownloads()
	{
		if (true)
		{
			return;
		}
			Connection connection = Services.h2DbCache.getConnection();
			PreparedStatement prepareStatement;
			ResultSet results;
			try
			{
				prepareStatement = connection.prepareStatement("select * from Download order by ADDED group by PRIORITY where DSTATE=?");
				prepareStatement.setInt(1, DownloadState.QUEUED.toInt());
				results = prepareStatement.executeQuery();
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
				return;
			}
			try (DbIterator<Download> dbIterator = new DbIterator<Download>(connection, results, DbObjects.PENDING_DOWNLOAD);)
			{
				while (dbIterator.hasNext())
				{
					if (downloads.size() >= Services.settings.maxDownloads.get())
					{
						dbIterator.close();
						return;
					}
					final Download next = dbIterator.next();
					Services.userThreads.execute(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								DownloadInstance createDownload = createDownload(next);
								if (createDownload != null)
								{
									createDownload.begin();
								}
							}
							catch (IOException e)
							{
								Services.logger.print(e);
							}
						}
					});
				}
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
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
