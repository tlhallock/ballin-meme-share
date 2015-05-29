package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.dmn.Notifications.NotificationListener;
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
	
	// Need to add a notifications listener...
	
	NotificationListener listener;
	
	public ChecksumManager()
	{
		Services.notifications.add(listener = new Notifications.NotificationListener()
		{
			@Override
			public void localsChanged()
			{
				kick();
			}

			@Override
			public void localDirectoryChanged(LocalDirectory local)
			{
				kick();
			}

			@Override
			public void fileAdded(SharedFile file)
			{
				kick();
			}

			@Override
			public void fileChanged(SharedFile file)
			{
				kick();
			}
		});
	}

	private void kick()
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
		while (true)
		{
			if (stop) return null;
			
			lock.lock();
			try
			{
				condition.await();
			}
			catch (InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted while waiting for condition.", e);
			}
			finally
			{
				lock.unlock();
			}
			if (stop) return null;

			LocalFile unChecksummedFile = DbFiles.getUnChecksummedFile();
			if (unChecksummedFile != null)
			{
				return unChecksummedFile;
			}
		}
	}

	private void updateChecksum(LocalFile sf)
	{
		String checksum = null;
		try
		{
			checksum = checksumBlocking(sf.getFsFile());
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to calculate checksum of " + sf, e);
		}
		if (checksum != null)
		{
			sf.setChecksum(checksum);
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

	public String checksumBlocking(File f) throws IOException
	{
		LogWrapper.getLogger().fine("Checksumming " + f);
		
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

		try (FileInputStream fis = new FileInputStream(f))
		{
			byte[] dataBytes = new byte[1024];

			int nread = 0;

			while ((nread = fis.read(dataBytes)) != -1)
			{
				digest.update(dataBytes, 0, nread);
			}

			String digestToString = digestToString(digest);
			LogWrapper.getLogger().fine("Done checksumming " + f + ": " + digestToString);
			return digestToString;
		}
	}
	
	public void quit()
	{
		lock.lock();
		stop = true;
		Services.notifications.remove(listener);
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
