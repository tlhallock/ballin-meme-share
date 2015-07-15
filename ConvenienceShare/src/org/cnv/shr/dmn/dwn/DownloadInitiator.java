
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.util.LogWrapper;

public class DownloadInitiator extends Thread
{
	private static final long INITIATOR_DELAY = 60 * 1000;
	
	long nextSync;
	long lastSync;
	
	Lock lock = new ReentrantLock();
	Condition condition = lock.newCondition();
	
	public void kick()
	{
		lock.lock();
		try
		{
			nextSync = 0;
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private void waitForNextSync() throws InterruptedException
	{
		lock.lock();
		try
		{
			lastSync = System.currentTimeMillis();
			nextSync = lastSync + INITIATOR_DELAY;
			
			for (;;)
			{
				long now = System.currentTimeMillis();
				long amountToWait = Math.max(nextSync - now, lastSync - now + 5 * 1000);
				if (amountToWait <= 0)
				{
					return;
				}
				condition.await(amountToWait, TimeUnit.MILLISECONDS);
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public void run()
	{
		try
		{
			for (;;)
			{
				waitForNextSync();

				int index = 0;
				LogWrapper.getLogger().info("Num downloads: " + Services.downloads.getNumActiveDownloads());
				LogWrapper.getLogger().info("Pending downloads:");
				LogWrapper.getLogger().info("------------------------------------------------------");
				try (DbIterator<Download> dbIterator = DbDownloads.listPendingDownloads();)
				{
					while (dbIterator.hasNext())
					{
						Download next = dbIterator.next();
						LogWrapper.getLogger().info("Pending download " + index++ + ": " + next.toString());
						Services.downloads.continueDownloadInstance(next);
					}
				}
				LogWrapper.getLogger().info("------------------------------------------------------");
				LogWrapper.getLogger().info("Done initiating downloads.");
			}
		}
		catch (InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Initiator interrupted.", e);
		}
	}
}
