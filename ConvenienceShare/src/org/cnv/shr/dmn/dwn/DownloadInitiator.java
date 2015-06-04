package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.ChecksumRequest;
import org.cnv.shr.util.LogWrapper;

public class DownloadInitiator extends TimerTask
{
	private void restart(final Download next)
	{
		if (Services.downloads.downloads.size() >= Services.settings.maxDownloads.get())
		{
			return;
		}
		
		try
		{
			DownloadInstance createDownload = Services.downloads.createDownload(next);
			if (createDownload == null)
			{
				return;
			}
			createDownload.continueDownload();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to begin download.", e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to send checksum request.", e);
		}
	}

	@Override
	public void run()
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				initiatePendingDownloads();
			}
		});
	}

	
	public void initiatePendingDownloads()
	{
		LogWrapper.getLogger().info("Pending downloads:");
		LogWrapper.getLogger().info("------------------------------------------------------");
		try (DbIterator<Download> dbIterator = DbDownloads.listPendingDownloads();)
		{
			while (dbIterator.hasNext())
			{
				if (Services.downloads.downloads.size() >= Services.settings.maxDownloads.get())
				{
					return;
				}
				Download next = dbIterator.next();
				LogWrapper.getLogger().info(next.toString());
				
				Services.userThreads.execute(new Runnable() {
					@Override
					public void run()
					{
						synchronized(Services.downloads)
						{
							restart(next);
						}
					}
				});
			}
		}
		LogWrapper.getLogger().info("------------------------------------------------------");
		LogWrapper.getLogger().info("Done initiating downloads.");
	}
}
