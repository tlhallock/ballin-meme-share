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
		lock.lock();
		while (queue.isEmpty())
		{
			try
			{
				condition.await();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			if (stop)
			{
				return null;
			}
		}

		Iterator<File> iterator = queue.iterator();
		File next = iterator.next();
		lock.unlock();
		
		return next;
	}

	void updateChecksum(File f)
	{
		long startTime = System.currentTimeMillis();
		String checksum;
		try
		{
			checksum = checksumBlocking(f);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		if (checksum == null)
		{
			return;
		}

		LocalFile l = Services.locals.getLocalFile(f);
		if (l == null)
		{
			Services.logger.logStream.println("File is not part of a shared directory: " + f);
			return;
		}
		l.setChecksum(checksum);
		l.setLastUpdated(startTime);

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
			Thread.sleep(Services.settings.checksumWait);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public String checksumBlocking(File f) throws IOException
	{
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			System.out.println("No SHA1 algorithm.");
			System.out.println("Quiting");
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
