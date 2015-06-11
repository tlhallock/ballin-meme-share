package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbIterator;
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
	
	private boolean stop;
	
	private DbIterator<LocalFile> iterator;
	
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
				iterator = DbFiles.getSomeUnchecksummedFiles();
				count++;
			}
			if (iterator.hasNext())
			{
				return iterator.next();
			}

			iterator.close();
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
				condition.await(1, TimeUnit.DAYS);
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
		try
		{
			checksum = checksumBlocking(sf.getFsFile(), Level.INFO);
			if (checksum != null)
			{
				sf.setChecksum(checksum);
			}
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to calculate or set checksum of " + sf, e);
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
			byte[] dataBytes = new byte[1024];

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
		iterator.close();
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
