
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



package org.cnv.shr.sync;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.msg.ListPath;
import org.cnv.shr.msg.PathList;
import org.cnv.shr.util.LogWrapper;

public class RemoteSynchronizerQueue implements Closeable
{
	private long MAX_TIME_TO_WAIT = 2 * 60 * 1000;
	private long lastCommunication;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private HashSet<String> queue = new HashSet<>();
	
	private HashMap<String, PathList> directories = new HashMap<>();
	Communication connection;
	RemoteDirectory root;
	
	public RemoteSynchronizerQueue(Communication c, final RemoteDirectory root)
	{
		this.connection = c;
		this.root = root;
	}
	
	private PathList waitForDirectory(final String path) throws IOException, InterruptedException
	{
		for (;;)
		{
			PathList directoryList = directories.get(path);
			if (directoryList != null && directoryList.listIsComplete())
			{
				directories.remove(path);
				return directoryList;
			}
			try
			{
				condition.await(20, TimeUnit.SECONDS);
			}
			catch (final InterruptedException e)
			{
				// TODO: Make sure this is handled well...
				LogWrapper.getLogger().log(Level.INFO, "Interrupted.", e);
				getConnection().close();
				throw e;
			}
			if (getConnection().isClosed() || System.currentTimeMillis() - lastCommunication > MAX_TIME_TO_WAIT)
			{
				throw new IOException("connection closed.");
			}
		}
	}
	
	PathList getDirectoryList(final PathElement pathElement) throws IOException, InterruptedException
	{
		final String path = pathElement.getFullPath();

		lock.lock();
		try
		{
			if (getConnection().isClosed())
			{
				throw new IOException("connection closed.");
			}
			
			try
			{
				ensureQueued(pathElement);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to queue directory list", e);
				return null;
			}
			return waitForDirectory(path);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void receiveList(final PathList list)
	{
		lock.lock();
		try
		{
			LogWrapper.getLogger().info("Received \"" + list.getCurrentPath() + "\"");
			PathList oldList = directories.put(list.getCurrentPath(), list);
			if (oldList != null)
			{
				list.merge(oldList);
			}
			lastCommunication = System.currentTimeMillis();
			
			if (list.listIsComplete())
			{
				queue.remove(list.getCurrentPath());
				condition.signalAll();
			}
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private void ensureQueued(final PathElement path) throws IOException, InterruptedException
	{
		String fullPath = path.getFullPath();
		
		if (queue.contains(fullPath) || directories.containsKey(fullPath))
		{
			return;
		}
		if (getConnection().isClosed())
		{
			return;
		}

		LogWrapper.getLogger().info("Queuing \"" + fullPath + "\"");
		lastCommunication = System.currentTimeMillis();
		getConnection().send(new ListPath(root, path));
		queue.add(fullPath);
	}
	
	public void queueDirectoryList(final PathElement path) throws IOException, InterruptedException
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
	
	@Override
	public void close()
	{
		lock.lock();
		try
		{
			Services.syncs.done(this);
			condition.signalAll();
			if (connection != null)
			{
				connection.finish();
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public String getUrl()
	{
		return connection.getUrl();
	}
	
	private Communication getConnection()
	{
			return connection;
	}
}
