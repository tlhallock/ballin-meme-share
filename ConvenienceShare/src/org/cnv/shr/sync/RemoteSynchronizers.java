package org.cnv.shr.sync;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.msg.DirectoryList;
import org.cnv.shr.msg.ListPath;

public class RemoteSynchronizers
{
	public Hashtable<String, RemoteSynchronizerQueue> synchronizers = new Hashtable<>();
	
	private String getKey(RemoteSynchronizerQueue s)
	{
		return getKey(s.communication, s.root);
	}
	private String getKey(Communication c, RemoteDirectory r)
	{
		return c.getUrl() + "::" + r.getName();
	}
	
	public RemoteSynchronizerQueue getSynchronizer(Communication c, RemoteDirectory r)
	{
		return synchronizers.get(getKey(c, r));
	}

	public RemoteSynchronizerQueue createRemoteSynchronizer(Machine remote, RemoteDirectory root) throws UnknownHostException, IOException
	{
		Communication c = Services.networkManager.openConnection(remote, false);
		if (c == null)
		{
			throw new IOException("Unable to connect to remote!");
		}
		RemoteSynchronizerQueue returnValue = new RemoteSynchronizerQueue(c, root);
		synchronizers.put(getKey(returnValue), returnValue);
		return returnValue;
	}
	
	public void done(RemoteSynchronizerQueue sync)
	{
		synchronizers.remove(getKey(sync));
	}

	public class RemoteSynchronizerQueue implements Closeable
	{
		private int errorCount = 0;
		private long MAX_TIME_TO_WAIT = 2 * 60 * 1000;
		private long lastCommunication;
		
		private Lock lock = new ReentrantLock();
		private Condition condition = lock.newCondition();
		private HashMap<String, DirectoryList> directories = new HashMap<>();
		Communication communication;
		RemoteDirectory root;
		private HashSet<String> queue = new HashSet<>();
		
		public RemoteSynchronizerQueue(Communication c, RemoteDirectory root)
		{
			communication = c;
			this.root = root;
		}
		
		private DirectoryList waitForDirectory(String path)
		{
			for (;;)
			{
				DirectoryList directoryList = directories.get(path);
				if (directoryList != null)
				{
					directories.remove(path);
					return directoryList;
				}
				try
				{
					condition.await(20, TimeUnit.SECONDS);
				}
				catch (InterruptedException e)
				{
					// TODO: Make sure this is handled well...
					e.printStackTrace();
					communication.close();
					return null;
				}
				if (communication.isClosed() || System.currentTimeMillis() - lastCommunication > MAX_TIME_TO_WAIT)
				{
					errorCount++;
					return null;
				}
			}
		}
		
		DirectoryList getDirectoryList(PathElement pathElement)
		{
			String path = pathElement.getFullPath();
			lock.lock();
			try
			{
				if (communication.isClosed())
				{
					return null;
				}
				ensureQueued(pathElement);
				return waitForDirectory(path);
			}
			finally
			{
				lock.unlock();
			}
		}
		
		public void receiveList(DirectoryList list)
		{
			lock.lock();
			try
			{
				directories.put(list.getCurrentPath(), list);
				queue.remove(list.getCurrentPath());
				lastCommunication = System.currentTimeMillis();
				condition.signalAll();
			}
			finally
			{
				lock.unlock();
			}
		}
		
		private void ensureQueued(PathElement path)
		{
			if (queue.contains(path.getFullPath()))
			{
				return;
			}
			if (communication.isClosed())
			{
				return;
			}
			lastCommunication = System.currentTimeMillis();
			communication.send(new ListPath(root, path));
			queue.add(path.getFullPath());
		}
		
		public void queueDirectoryList(PathElement path)
		{
			lock.lock();
			try
			{
				ensureQueued(path);
			}
			finally
			{
				lock.unlock();
			}
		}
		
		public void close()
		{
			lock.lock();
			try
			{
				done(this);
				condition.signalAll();
				communication.finish();
			}
			finally
			{
				lock.unlock();
			}
		}
	}
	
	public void closeAll()
	{
		for (RemoteSynchronizerQueue s : synchronizers.values())
		{
			s.close();
		}
	}
}
