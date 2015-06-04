package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.LogWrapper;

public class DownloadManager
{
	// Need to remove old ones...
	HashMap<FileEntry, DownloadInstance> downloads = new HashMap<>();
	DownloadInitiator initiator = new DownloadInitiator();

	public DownloadInstance download(SharedFile remoteFile) throws UnknownHostException, IOException
	{
		if (remoteFile.isLocal())
		{
			JOptionPane.showMessageDialog(null,
					"Unable to download local file: " + remoteFile.getRootDirectory().getPathElement().getFullPath() + ":" + remoteFile.getPath().getFullPath(),
					"Unable to download local file.",
					JOptionPane.INFORMATION_MESSAGE);
			LogWrapper.getLogger().info("Trying to download local file " + remoteFile);
			return null;
		}
		LogWrapper.getLogger().info("Trying to download " + remoteFile);
		return download((RemoteFile) remoteFile);
	}

	public DownloadInstance download(RemoteFile file) throws UnknownHostException, IOException
	{
		return createDownload(new Download(file));
	}

	synchronized DownloadInstance createDownload(Download d) throws UnknownHostException, IOException
	{
		DownloadInstance prev = downloads.get(d.getFile().getId());
		if (prev != null)
		{
			LogWrapper.getLogger().info("Already downloading.");
			return null;
		}

		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
		{
			d.save(c);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write new download to database.", e);
		}

		String checksum = d.getFile().getChecksum();
		if (checksum == null)
		{
			LogWrapper.getLogger().info("File is not checksummed.");
			initiator.requestChecksum(d.getFile());
			return null;
		}

		if (downloads.size() >= Services.settings.maxDownloads.get())
		{
			return null;
		}

		LogWrapper.getLogger().info("Creating download instance");
		DownloadInstance instance = new DownloadInstance(d);
		Services.notifications.downloadAdded(instance);
		downloads.put(d.getFile().getFileEntry(), instance);
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					instance.continueDownload();
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to download.", e);
				}
			}
		});
		return instance;
	}

	public synchronized void remove(DownloadInstance downloadInstance)
	{
		downloads.remove(new SharedFileId(downloadInstance.getDownload().getFile()));
		if (downloads.size() >= Services.settings.maxDownloads.get())
		{
			return;
		}
		initiator.initiatePendingDownloads();
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

	public synchronized DownloadInstance getDownloadInstanceForGui(FileEntry descriptor)
	{
		DownloadInstance downloadInstance = downloads.get(descriptor);
		if (downloadInstance == null)
		{
			return null;
		}
		return downloadInstance;
	}
	public synchronized DownloadInstance getDownloadInstance(FileEntry descriptor, Communication connection)
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


	public void startDownloadInitiator()
	{
		Services.timer.scheduleAtFixedRate(initiator, 1000, 10 * 60 * 1000);
	}

	public void quitAllDownloads()
	{
		for (DownloadInstance instance : downloads.values())
		{
			instance.fail("Closing all downloads.");
		}
	}

	public void initiatePendingDownloads()
	{
		initiator.initiatePendingDownloads();
	}
}
