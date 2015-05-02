package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.stng.Settings;

public class ChecksumManager extends Thread
{
	private HashSet<File> queue = new HashSet<>();
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	private boolean stop;

	@Override
	public void run()
	{
		File f;
		while ((f = getNextFile()) != null)
		{
			updateChecksum(f);
			beNice();
		}
	}
	
	private File getNextFile()
	{
		File next = null;
		
		lock.lock();
		try
		{
			while (queue.isEmpty())
			{
				try
				{
					condition.await();
				}
				catch (InterruptedException e)
				{
					Services.logger.logStream.println("Interrupted while waiting for condition.");
					e.printStackTrace(Services.logger.logStream);
				}
				if (stop)
				{
					return next;
				}
			}

			Iterator<File> iterator = queue.iterator();
			next = iterator.next();
		}
		finally
		{
			lock.unlock();
		}

		return next;
	}

	private void updateChecksum(File f)
	{
		long startTime = System.currentTimeMillis();
		String checksum;
		try
		{
			checksum = checksumBlocking(f);
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to calculate checksum of " + f);
			e.printStackTrace(Services.logger.logStream);
			return;
		}
		if (checksum == null)
		{
			return;
		}

		LocalFile l = Services.locals.getLocalFile(f);
		if (l == null)
		{
			Services.locals.getLocalFile(f);
			Services.logger.logStream.println("File is not part of a shared directory: " + f);
			return;
		}
		l.setChecksum(startTime, checksum);

		lock.lock();
		try
		{
			queue.remove(f);
		}
		finally
		{
			lock.unlock();
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
			Services.logger.logStream.println("Interrupted while waiting.");
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public String checksumBlocking(File f) throws IOException
	{
		Services.logger.logStream.println("Checksumming " + f);
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		}
		catch (NoSuchAlgorithmException e)
		{
			Services.logger.logStream.println("No SHA1 algorithm.\nQuitting");
			e.printStackTrace(Services.logger.logStream);
			Main.quit();
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

			byte[] mdbytes = digest.digest();

			// convert the byte to hex format
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++)
			{
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}

			Services.logger.logStream.println("Done checksumming " + f + ": " + sb.toString());
			return sb.toString();
		}
	}
	
	public void quit()
	{
		lock.lock();
		stop = true;
		condition.signalAll();
		lock.unlock();
	}

	public void checksum(File f)
	{
		lock.lock();
		try
		{
			queue.add(f);
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
}
