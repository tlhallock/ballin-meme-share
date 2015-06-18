
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

public class DownloadManager
{
	// Need to remove old ones...
	HashMap<FileEntry, DownloadInstance> downloads = new HashMap<>();
	DownloadInitiator initiator = new DownloadInitiator();
	ChecksumRequester requester = new ChecksumRequester();

	public DownloadInstance download(SharedFile remoteFile) throws UnknownHostException, IOException
	{
		return download(remoteFile, false);
	}
	public DownloadInstance download(SharedFile remoteFile, boolean force) throws UnknownHostException, IOException
	{
		if (remoteFile.isLocal())
		{
			JOptionPane.showMessageDialog(Services.notifications.getCurrentContext(),
					"Unable to download local file: " + remoteFile.getRootDirectory().getPathElement().getFullPath() + ":" + remoteFile.getPath().getFullPath(),
					"Unable to download local file.",
					JOptionPane.INFORMATION_MESSAGE);
			LogWrapper.getLogger().info("Trying to download local file " + remoteFile);
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
//		// Need to make a frame for this to keep all when several turn out to be the same.
//		// Should just copy the local file...
//		// Need to make show local copy...
//		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(Services.notifications.getCurrentContext(), 
//				"The file you requested to download is similar to you have locally.\n"
//				+ "Remote file: " + remoteFile + ".\n"
//				+ "Local file: " + file + ".\n"
//				+ "Both checksums are " + checksum + " and both file sizes are " + remoteFile.getFileSize() + " (" + Misc.formatDiskUsage(remoteFile.getFileSize()) + ").\n"
//				+ "Are you sure you would like to download the remote file?",
//				"Downloading file that you may already have.",
//				JOptionPane.YES_NO_OPTION,
//				JOptionPane.WARNING_MESSAGE))
//		{
//			return false;
//		}
		
		AlreadyDownloadedFrame.showAlreadyPresent((RemoteFile) remoteFile, file);

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
		
		if (!force && alreadyHaveCopy(d.getFile(), checksum, d.getFile().getFileSize()))
		{
			return null;
		}
		
		DownloadInstance prev = downloads.get(d.getFile().getFileEntry());
		if (prev != null)
		{
			LogWrapper.getLogger().info("Already downloading.");
			return null;
		}

		d.tryToSave();

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
		requester.start();
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
