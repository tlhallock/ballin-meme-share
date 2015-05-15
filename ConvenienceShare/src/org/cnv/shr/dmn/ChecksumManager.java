package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.stng.Settings;

public class ChecksumManager extends Thread
{
	private HashMap<String, SharedFile> queue = new HashMap<>();
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	private boolean stop;

	@Override
	public void run()
	{
		Entry<String, SharedFile> f;
		while ((f = getNextFile()) != null)
		{
			updateChecksum(f.getKey(), f.getValue());
			beNice();
		}
	}
	
	private Entry<String, SharedFile> getNextFile()
	{
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
					Services.logger.println("Interrupted while waiting for condition.");
					Services.logger.print(e);
				}
				if (stop)
				{
					return null;
				}
			}

			return queue.entrySet().iterator().next();
		}
		finally
		{
			lock.unlock();
		}
	}

	private void updateChecksum(String file, SharedFile sf)
	{
		String checksum = null;
		try
		{
			checksum = checksumBlocking(new File(file));
		}
		catch (Exception e)
		{
			Services.logger.println("Unable to calculate checksum of " + file);
			Services.logger.print(e);
		}
		if (checksum != null)
		{
			sf.setChecksum(checksum);
		}

		lock.lock();
		try
		{
			queue.remove(file);
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
			Services.logger.println("Interrupted while waiting.");
			Services.logger.print(e);
		}
	}

	public String checksumBlocking(File f) throws IOException
	{
//		Services.logger.println("Checksumming " + f);
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		}
		catch (NoSuchAlgorithmException e)
		{
			Services.logger.println("No SHA1 algorithm.\nQuitting");
			Services.logger.print(e);
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
			
//			Services.logger.println("Done checksumming " + f + ": " + sb.toString());
			return digestToString(digest);
		}
	}
	
	public void quit()
	{
		lock.lock();
		stop = true;
		condition.signalAll();
		lock.unlock();
	}

	public void checksum(SharedFile sf, File f)
	{
		lock.lock();
		try
		{
			try
			{
				queue.put(f.getCanonicalPath(), sf);
			}
			catch (IOException e)
			{
				queue.put(f.getAbsolutePath(), sf);
				Services.logger.print(e);
			}
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
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
