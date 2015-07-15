package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public enum AlreadyDownloadedAction
{
	Ask,
	Copy_Local_File,
	Cancel_Download,
	;

	public static void copyLocalFile(RemoteFile remote, LocalFile local)
	{
		String prevName = Thread.currentThread().getName();
		Thread.currentThread().setName("Move thread for " + remote.getChecksum());
		Download download = new Download(remote);
		download.tryToSave();
		Path targetFile = download.getTargetFile();
		Misc.ensureDirectory(targetFile, true);
		try
		{
			LogWrapper.getLogger().info("Moving " + local.getFsFile());
			Files.copy(local.getFsFile(), targetFile, StandardCopyOption.REPLACE_EXISTING);
			download.setState(DownloadState.ALL_DONE);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to copy " + local.getFsFile() + " to " + targetFile, e);
			try
			{
				Services.downloads.download(remote, false);
			}
			catch (IOException e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to download download from failed copy.", e1);
			}
		}
		// Services.notifications.downloadsChanged();
		Thread.currentThread().setName(prevName);
	}

	public static AlreadyDownloadedAction getCurrentAction()
	{
		String action = Services.settings.downloadPresentAction.get();
		AlreadyDownloadedAction valueOf = valueOf(action);
		if (valueOf == null)
		{
			valueOf = Ask;
			Services.settings.downloadPresentAction.set(valueOf.name());
		}
		LogWrapper.getLogger().info("Current action for downloads already present on local filesystem is " + valueOf.name());
		return valueOf;
	}

	public static void downloadEmptyFile(RemoteFile remote)
	{
		String prevName = Thread.currentThread().getName();
		Thread.currentThread().setName("Move thread for " + remote.getChecksum());
		Download download = new Download(remote);
		download.tryToSave();
		Path targetFile = download.getTargetFile();
		Misc.ensureDirectory(targetFile, true);
		try
		{
			LogWrapper.getLogger().info("touching " + targetFile);
			Files.createFile(targetFile);
			download.setState(DownloadState.ALL_DONE);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to touch " + targetFile, e);
		}
		// Services.notifications.downloadsChanged();
		Thread.currentThread().setName(prevName);
	}
}
