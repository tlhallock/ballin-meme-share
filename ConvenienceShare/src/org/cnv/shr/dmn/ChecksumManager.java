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

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.stng.Settings;

public class ChecksumManager extends Thread
{
	private HashMap<String, LocalDirectory> queue = new HashMap<>();
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	private boolean stop;

	@Override
	public void run()
	{
		Entry<String, LocalDirectory> f;
		while ((f = getNextFile()) != null)
		{
			updateChecksum(f.getKey(), f.getValue());
			beNice();
		}
	}
	
	private Entry<String, LocalDirectory> getNextFile()
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

	private void updateChecksum(String file, LocalDirectory l)
	{
		long startTime = System.currentTimeMillis();
		String checksum;
		try
		{
			checksum = checksumBlocking(l, new File(file));
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to calculate checksum of " + file);
			e.printStackTrace(Services.logger.logStream);
			return;
		}
		if (checksum == null)
		{
			return;
		}

		SharedFile sf = l.getFile(file);
		if (sf != null)
		{
			sf.setChecksum(checksum);
		}
		else
		{
			Services.logger.logStream.println("File went missing: " + file);
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
			Services.logger.logStream.println("Interrupted while waiting.");
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public String checksumBlocking(LocalDirectory l, File f) throws IOException
	{
//		Services.logger.logStream.println("Checksumming " + f);
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

//			Services.logger.logStream.println("Done checksumming " + f + ": " + sb.toString());
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

	public void checksum(LocalDirectory local, File f)
	{
		lock.lock();
		try
		{
			try
			{
				queue.put(f.getCanonicalPath(), local);
			}
			catch (IOException e)
			{
				queue.put(f.getAbsolutePath(), local);
				e.printStackTrace();
			}
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
}
