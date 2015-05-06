package org.cnv.shr.lcl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.msg.DirectoryList;
import org.cnv.shr.msg.ListDirectory;

public class RemoteSynchronizers
{
	public Hashtable<String, RemoteSynchronizer> synchronizers = new Hashtable<>();
	
	private String getKey(RemoteSynchronizer s)
	{
		return getKey(s.communication, s.root);
	}
	private String getKey(Communication c, RemoteDirectory r)
	{
		return c.getUrl() + "::" + r.getName();
	}
	
	public RemoteSynchronizer getSynchronizer(Communication c, RemoteDirectory r)
	{
		return synchronizers.get(getKey(c, r));
	}

	public RemoteSynchronizer createRemoteSynchronizer(Machine remote, RemoteDirectory root) throws UnknownHostException, IOException
	{
		Communication c = Services.networkManager.openConnection(remote.getIp(), remote.getPort());
		RemoteSynchronizer returnValue = new RemoteSynchronizer(c, root);
		synchronizers.put(getKey(returnValue), returnValue);
		return returnValue;
	}
	
	public void done(RemoteSynchronizer sync)
	{
		synchronizers.remove(getKey(sync));
	}

	public class RemoteSynchronizer
	{
		private Lock lock = new ReentrantLock();
		private Condition condition = lock.newCondition();
		private HashMap<String, DirectoryList> directories = new HashMap<>();
		Communication communication;
		RemoteDirectory root;
		
		public RemoteSynchronizer(Communication c, RemoteDirectory root)
		{
			communication = c;
			this.root = root;
		}
		
		private DirectoryList waitForDirectory(String path)
		{
			for (;;)
			{
				try
				{
					condition.await(60, TimeUnit.SECONDS);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				DirectoryList directoryList = directories.get(path);
				if (directoryList != null)
				{
					return directoryList;
				}
			}
		}
		
		DirectoryList getDirectoryList(String path)
		{
			lock.lock();
			try
			{
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
				condition.notifyAll();
			}
			finally
			{
				lock.unlock();
			}
		}
		
		void queueDirectoryList(RemoteDirectory remote, PathElement path)
		{
			communication.send(new ListDirectory(remote, path));
		}
	}
}
