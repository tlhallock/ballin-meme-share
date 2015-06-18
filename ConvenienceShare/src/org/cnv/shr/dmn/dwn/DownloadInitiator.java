
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
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
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
			DownloadInstance createDownload = Services.downloads.createDownload(next, false);
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
