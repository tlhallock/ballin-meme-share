
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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionParams.KeepOpenConnectionParams;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AlreadyDownloadedFrame;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.NonRejectingExecutor;

public class DownloadManager
{
	// Need to remove old ones...
	private Hashtable<FileEntry, DownloadInstance> downloads = new Hashtable<>();
	DownloadInitiator initiator = new DownloadInitiator();
	ChecksumRequester requester = new ChecksumRequester();
	public NonRejectingExecutor downloadThreads = new NonRejectingExecutor("dwnld", Services.settings.maxDownloads.get());

	public void download(SharedFile remoteFile) throws UnknownHostException, IOException
	{
		download(remoteFile, false);
	}
	public void download(SharedFile remoteFile, boolean force)
	{
		if (remoteFile.isLocal())
		{
			LocalFile localFile = (LocalFile) remoteFile;
			JOptionPane.showMessageDialog(Services.notifications.getCurrentContext(),
					"Unable to download local file: " + remoteFile.getRootDirectory().getPath() + ":" + remoteFile.getPath().getFullPath() + "\n" +
					"Instead we will open it.",
					"Unable to download local file.",
					JOptionPane.INFORMATION_MESSAGE);
			LogWrapper.getLogger().info("Trying to download local file " + remoteFile);
			Misc.nativeOpen(localFile.getFsFile(), false);
			return;
		}
		LogWrapper.getLogger().info("Trying to download " + remoteFile);
		download((RemoteFile) remoteFile, force);
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
	
	public void download(RemoteFile file, boolean force)
	{
		createDownload(new Download(file), force);
	}

	void createDownload(Download d, boolean force)
	{
		String checksum = d.getFile().getChecksum();
		
		if (checksum == null || checksum.length() != SharedFile.CHECKSUM_LENGTH)
		{
			requester.requestChecksum(d.getFile());
			return;
		}
		
		requester.fileHasChecksum(d.getFile());
		
		if (d.getFile().getFileSize() == 0)
		{
			AlreadyDownloadedAction.downloadEmptyFile(d.getFile());
			return;
		}
		
		if (!force && alreadyHaveCopy(d.getFile(), checksum, d.getFile().getFileSize()))
		{
			return;
		}
		
		FileEntry fileEntry = d.getFile().getFileEntry();
		if (downloads.get(fileEntry) != null)
		{
			LogWrapper.getLogger().info("Already downloading.");
			return;
		}

		d.tryToSave();

		// Should make the initiator a listener...
		if (getNumActiveDownloads() < Services.settings.maxDownloads.get())
		{
			initiator.kick();
		}
		Services.notifications.downloadsChanged();
	}
	
	void continueDownloadInstance(Download download)
	{
		FileEntry fileEntry = download.getFile().getFileEntry();

		DownloadInstance downloadInstance = downloads.get(fileEntry);
		if (downloadInstance != null)
		{
			LogWrapper.getLogger().info("Already downloading.");
			downloadInstance.continueDownload();
			return;
		}

		synchronized (creationSync)
		{
			downloadInstance = downloads.get(fileEntry);
			if (downloadInstance != null)
			{
				LogWrapper.getLogger().info("Already downloading.");
				downloadInstance.continueDownload();
				return;
			}


			if (!startCreating(fileEntry))
			{
				LogWrapper.getLogger().info("Already creating download for " + fileEntry);
				return;
			}
		}

		if (getNumActiveDownloads() + creationSync.size() >= Services.settings.maxDownloads.get())
		{
			doneCreating(fileEntry);
			return;
		}
		
		Machine machine = download.getFile().getRootDirectory().getMachine();
		Services.networkManager.openConnection(new KeepOpenConnectionParams(null, machine, false, "Download file")
		{
			@Override
			public void opened(Communication connection) throws Exception
			{
				Seeder primarySeeder = new Seeder(machine, connection);
				LogWrapper.getLogger().info("Creating download instance");
				DownloadInstance instance = new DownloadInstance(download, primarySeeder);
				downloads.put(fileEntry, instance);
				doneCreating(fileEntry);

				instance.allocate();
				instance.recover();
				instance.continueDownload();
				Services.notifications.downloadAdded(instance);
			}

			public void failed()
			{
				doneCreating(fileEntry);
			}
		});
	}
	public int getNumActiveDownloads()
	{
		return downloads.size();
	}

	public void remove(FileEntry entry)
	{
		downloads.remove(entry);
		if (getNumActiveDownloads() >= Services.settings.maxDownloads.get())
		{
			return;
		}
		initiator.kick();
	}

	public LinkedList<DownloadInstance> getDownloadInstances(Communication c)
	{
		LinkedList<DownloadInstance> returnValue = new LinkedList<>();

		for (DownloadInstance instance : ((Map<FileEntry, DownloadInstance>) downloads.clone()).values())
		{
			if (instance.contains(c))
			{
				returnValue.add(instance);
			}
		}
		return returnValue;
	}

	public DownloadInstance getDownloadInstanceForGui(Download download)
	{
		return getDownloadInstanceForGui(download.getFile().getFileEntry());
	}
	
	public DownloadInstance getDownloadInstanceForGui(FileEntry descriptor)
	{
		DownloadInstance downloadInstance = downloads.get(descriptor);
		if (downloadInstance == null)
		{
			LogWrapper.getLogger().finest("Unable to find download for " + descriptor);
			return null;
		}
		return downloadInstance;
	}
	
	public DownloadInstance getDownloadInstance(FileEntry descriptor, Communication connection)
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
		initiator.start();
		requester.start();
	}

	public void quitAllDownloads()
	{
		for (DownloadInstance instance : downloads.values())
		{
			instance.fail("Closing all downloads.");
		}
		requester.interrupt();
		initiator.interrupt();
	}

	public void initiatePendingDownloads()
	{
		initiator.kick();
	}
	
	public boolean hasPendingChecksumRequest(SharedFileId id)
	{
		return requester.hasSharedPendingId(id);
	}
	
	
	
	
	private HashMap<FileEntry, Long> creationSync = new HashMap<>();
	private boolean startCreating(FileEntry newOne)
	{
		Long last = null;
		long now;
		synchronized (creationSync)
		{
			last = creationSync.put(newOne, now = System.currentTimeMillis());
		}
		return last == null || last + 10 * 60 * 100 < now;
	}
	private void doneCreating(FileEntry newOne)
	{
		synchronized (creationSync)
		{
			creationSync.remove(newOne);
		}
	}
	
	
	
	
	
	// Should probably revert everything from here: the real problem was creation
	
	private Hashtable<Integer, GuiInfo> guiInfo = new Hashtable<>();
	public void updateGuiInfo(Download instance, GuiInfo info)
	{
		guiInfo.put(instance.getId(), info);
	}
	public void removeGuiInfo(Integer id)
	{
		guiInfo.remove(id);
	}
	public GuiInfo getGuiInfo(Download d)
	{
		GuiInfo guiInfo2 = guiInfo.get(d.getId());
		if (guiInfo2 == null)
		{
			return new GuiInfo("0", "N/A", "0.0");
		}
		return guiInfo2;
	}
	
	public static final class GuiInfo
	{
		public final String numSeeders;
		public final String speed;
		public final String percentComplete;
		
		public GuiInfo(String numSeeders, String speed, String percentComplete)
		{
			this.numSeeders = numSeeders;
			this.speed = speed;
			this.percentComplete = percentComplete;
		}
	}
}
