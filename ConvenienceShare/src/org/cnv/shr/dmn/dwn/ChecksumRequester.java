package org.cnv.shr.dmn.dwn;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.not.NotificationListener;
import org.cnv.shr.dmn.not.NotificationListenerAdapter;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.ChecksumRequest;
import org.cnv.shr.util.LogWrapper;

public class ChecksumRequester extends Thread
{
	// TODO: go to database...
	
	private LinkedBlockingDeque<RemoteFile> deque = new LinkedBlockingDeque<>();
	private HashSet<SharedFileId> pending = new HashSet<>();
	private Communication openConnection;

	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private boolean requestCompleted;
	
	private NotificationListener listener = new NotificationListenerAdapter()
	{
		public void connectionClosed(Communication c)
		{
			if (openConnection == null)
			{
				return;
			}
			if (!openConnection.equals(c))
			{
				return;
			}
			lock.lock();
			try
			{
				requestCompleted = true;
			}
			finally
			{
				lock.unlock();
			}
		};
	};

	{
		Services.notifications.add(listener);
	}

	public void requestChecksum(RemoteFile remote)
	{
		LogWrapper.getLogger().info("File is not checksummed.");
		deque.offerLast(remote);
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				RemoteFile remote = deque.takeFirst();
				SharedFileId fileid = new SharedFileId(remote);
				pending.add(fileid);
				requestCompleted = false;
				LogWrapper.getLogger().info("Requesting checksum for " + fileid);
				openConnection = Services.networkManager.openConnection(remote.getRootDirectory().getMachine(), false);
				if (openConnection == null)
				{
					LogWrapper.getLogger().info("Unable to request checksum of " + fileid);
					continue;
				}
				openConnection.send(new ChecksumRequest(remote));
				
				lock.lock();
				try
				{
					while (!requestCompleted)
					{
						LogWrapper.getLogger().info("Still waiting for checksum of " + fileid);
						condition.await(10, TimeUnit.SECONDS);
					}
				}
				finally
				{
					lock.unlock();
					openConnection = null;
					pending.remove(fileid);
				}
				LogWrapper.getLogger().info("Done waiting for checksum of " + fileid);
			}
			catch (InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted, quitting.", e);
				return;
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to send checksum request.", e);
			}
		}
	}
	
	public boolean hasSharedPendingId(SharedFileId id)
	{
		return pending.remove(id);
	}
}
