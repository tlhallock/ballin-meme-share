
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



package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.dmn.not.NotificationListenerAdapter;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.LogWrapper;

public class ChecksumManager extends Thread
{
	// Should get this directory from the database...
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private HashMap<String, Long> errors = new HashMap<>();
	
	private boolean stop;
	
	private Iterator<Integer> iterator;
	
	public ChecksumManager()
	{
		setName("ChecksumManager");
		Services.notifications.add(new NotificationListenerAdapter()
		{
			@Override
			public void localsChanged()                             { kick(); }
			@Override
			public void localDirectoryChanged(LocalDirectory local) { kick(); }
			@Override
			public void fileAdded(SharedFile file)                  { kick(); }
			@Override
			public void fileChanged(SharedFile file)                { kick(); }
		});
	}

	void kick()
	{
		lock.lock();
		condition.signalAll();
		lock.unlock();
	}

	@Override
	public void run()
	{
		LocalFile f;
		while ((f = getNextFile()) != null)
		{
			updateChecksum(f);
			beNice();
		}
	}
	
	private LocalFile getNextFile()
	{
		int count = 0;
		while (true)
		{
			if (stop) return null;
			if (iterator == null)
			{
				iterator = DbFiles.getSomeUnchecksummedFiles().iterator();
				count++;
			}
			LocalFile next;
			while (iterator.hasNext())
			{
				Integer next2 = iterator.next();
				LocalFile file = (LocalFile) DbFiles.getFile(next2);
				// We will be kicked when the synchronizing is done anyway...
				if (file != null && !file.getRootDirectory().isSynchronizing())
				{
					return file;
				}
			}

			iterator = null;
			if (count < 2)
			{
				// results are paged, don't wait yet
				continue;
			}
			
			lock.lock();
			try
			{
				// Wait for a notification that there are new files...
				condition.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted while waiting for condition.", e);
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	private void updateChecksum(LocalFile sf)
	{
		String checksum = null;
		
		String errorsKey = sf.getFsFile().toString();
		Long long1 = errors.get(errorsKey);
		if (long1 != null)
		{
			if (long1 >= System.currentTimeMillis() - 5 * 60 * 1000)
			{
				return;
			}
			errors.remove(errorsKey);
		}
		
		try
		{
			checksum = checksumBlocking(sf.getFsFile(), Level.INFO);
			if (checksum != null)
			{
				sf.setChecksum(checksum);
			}
			errors.remove(errorsKey);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to calculate or set checksum of " + sf, e);
			errors.put(errorsKey, System.currentTimeMillis());
			iterator = null;
		}
	}
	
	private void beNice()
	{
		if (stop)
		{
			return;
		}
		try
		{
			Thread.sleep(Services.settings.checksumWait.get());
		}
		catch (InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Interrupted while waiting.", e);
		}
	}

	public String checksumBlocking(Path f, Level level) throws IOException
	{
		LogWrapper.getLogger().log(level, "Checksumming " + f + " [" + new DiskUsage(Files.size(f)) + "]");

		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		}
		catch (NoSuchAlgorithmException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "No SHA1 algorithm.\nQuitting", e);
			Services.quiter.quit();
			return null;
		}

		try (InputStream fis = Files.newInputStream(f))
		{
			byte[] dataBytes = new byte[8192];

			int nread = 0;

			while ((nread = fis.read(dataBytes)) != -1)
			{
				digest.update(dataBytes, 0, nread);
			}

			String digestToString = digestToString(digest);
			LogWrapper.getLogger().log(level, "Done checksumming : " + digestToString);
			return digestToString;
		}
	}
	
	public void quit()
	{
		lock.lock();
		stop = true;
		condition.signalAll();
		lock.unlock();
	}

	public static String digestToString(MessageDigest digest)
	{
		byte[] mdbytes = digest.digest();

		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < mdbytes.length; i++)
		{
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}
}
