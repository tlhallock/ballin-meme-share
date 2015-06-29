
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AlreadyDownloadedFrame;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DownloadManager
{
	// Need to remove old ones...
	HashMap<FileEntry, DownloadInstance> downloads = new HashMap<>();
	DownloadInitiator initiator = new DownloadInitiator();
	ChecksumRequester requester = new ChecksumRequester();
	public ScheduledExecutorService downloadThreads = Executors.newScheduledThreadPool(0);

	public DownloadInstance download(SharedFile remoteFile) throws UnknownHostException, IOException
	{
		return download(remoteFile, false);
	}
	public DownloadInstance download(SharedFile remoteFile, boolean force) throws UnknownHostException, IOException
	{
		if (remoteFile.isLocal())
		{
			LocalFile localFile = (LocalFile) remoteFile;
			JOptionPane.showMessageDialog(Services.notifications.getCurrentContext(),
					"Unable to download local file: " + remoteFile.getRootDirectory().getPathElement().getFullPath() + ":" + remoteFile.getPath().getFullPath() + "\n" +
					"Instead we will open it.",
					"Unable to download local file.",
					JOptionPane.INFORMATION_MESSAGE);
			LogWrapper.getLogger().info("Trying to download local file " + remoteFile);
			Misc.nativeOpen(localFile.getFsFile(), false);
			return null;
		}
		LogWrapper.getLogger().info("Trying to download " + remoteFile);
		return download((RemoteFile) remoteFile, force);
	}
	
	private static boolean alreadyHaveCopy(SharedFile remoteFile, String checksum, long fileSize)
	{
		LocalFile file = DbFiles.getFile(checksum, remoteFile.getFileSize());
		if (file == null)
		{
			return false;
		}
		
		switch (AlreadyDownloadedAction.getCurrentAction())
		{
		case Ask:
			AlreadyDownloadedFrame.showAlreadyPresent((RemoteFile) remoteFile, file);
			break;
		case Copy_Local_File:
			AlreadyDownloadedAction.copyLocalFile((RemoteFile) remoteFile, file);
			break;
		case Cancel_Download:
			break;
		}
		return true;
	}
	
	public DownloadInstance download(RemoteFile file, boolean force) throws UnknownHostException, IOException
	{
		return createDownload(new Download(file), force);
	}

	synchronized DownloadInstance createDownload(Download d, boolean force) throws UnknownHostException, IOException
	{
		String checksum = d.getFile().getChecksum();
		
		if (checksum == null || checksum.length() != SharedFile.CHECKSUM_LENGTH)
		{
			requester.requestChecksum(d.getFile());
			return null;
		}
		if (d.getFile().getFileSize() == 0)
		{
			AlreadyDownloadedAction.downloadEmptyFile(d.getFile());
			return null;
		}
		
		if (!force && alreadyHaveCopy(d.getFile(), checksum, d.getFile().getFileSize()))
		{
			return null;
		}
		
		DownloadInstance prev = downloads.get(d.getFile().getFileEntry());
		if (prev != null)
		{
			LogWrapper.getLogger().info("Already downloading.");
			prev.continueDownload();
			return null;
		}

		d.tryToSave();

		if (downloads.size() >= Services.settings.maxDownloads.get())
		{
			return null;
		}

		LogWrapper.getLogger().info("Creating download instance");
		DownloadInstance instance = new DownloadInstance(d);
		instance.allocate();
		instance.recover();
		downloads.put(d.getFile().getFileEntry(), instance);
		Services.notifications.downloadAdded(instance);
		instance.continueDownload();
		return instance;
	}

	public synchronized void remove(DownloadInstance downloadInstance)
	{
		downloads.remove(downloadInstance.getDownload().getFile().getFileEntry());
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

	public synchronized DownloadInstance getDownloadInstanceForGui(Download download)
	{
		FileEntry fileEntry = download.getFile().getFileEntry();
		DownloadInstance downloadInstance = downloads.get(fileEntry);
		if (downloadInstance == null)
		{
			DownloadInstance instance;
			try
			{
				instance = new DownloadInstance(download);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to create download.", e);
				return null;
			}
			downloads.put(fileEntry, instance);
		}
		return downloadInstance;
	}
	
	public synchronized DownloadInstance getDownloadInstanceForGui(FileEntry descriptor)
	{
		DownloadInstance downloadInstance = downloads.get(descriptor);
		if (downloadInstance == null)
		{
			LogWrapper.getLogger().fine("Unable to find download for " + descriptor);
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
		Services.downloads.downloadThreads.schedule(new Runnable() {
			@Override
			public void run()
			{
				Services.timer.scheduleAtFixedRate(initiator, 1000, 10 * 60 * 1000);
				requester.start();
			}}, 10, TimeUnit.SECONDS);
	}

	public void quitAllDownloads()
	{
		for (DownloadInstance instance : downloads.values())
		{
			instance.fail("Closing all downloads.");
		}
		requester.interrupt();
	}

	public void initiatePendingDownloads()
	{
		initiator.initiatePendingDownloads();
	}
	
	public boolean hasPendingChecksumRequest(SharedFileId id)
	{
		return requester.hasSharedPendingId(id);
	}
}
