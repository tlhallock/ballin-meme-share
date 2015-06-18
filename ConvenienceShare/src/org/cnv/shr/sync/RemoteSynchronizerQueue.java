
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
	Communication communication;
	RemoteDirectory root;
	
	public RemoteSynchronizerQueue(final Communication c, final RemoteDirectory root)
	{
		communication = c;
		this.root = root;
	}
	
	private PathList waitForDirectory(final String path) throws IOException, InterruptedException
	{
		for (;;)
		{
			final PathList directoryList = directories.get(path);
			if (directoryList != null)
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
				communication.close();
				throw e;
			}
			if (communication.isClosed() || System.currentTimeMillis() - lastCommunication > MAX_TIME_TO_WAIT)
			{
				throw new IOException("connection closed.");
			}
		}
	}
	
	PathList getDirectoryList(final PathElement pathElement) throws IOException, InterruptedException
	{
		final String path = pathElement.getFullPath();

		if (communication.isClosed())
		{
			throw new IOException("connection closed.");
		}
		lock.lock();
		try
		{
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
	
	private void ensureQueued(final PathElement path) throws IOException
	{
		if (queue.contains(path.getFullPath())
				|| directories.containsKey(path.getFullPath()))
		{
			return;
		}
		if (communication.isClosed())
		{
			return;
		}

		LogWrapper.getLogger().info("Queuing \"" + path.getFullPath() + "\"");
		lastCommunication = System.currentTimeMillis();
		communication.send(new ListPath(root, path));
		queue.add(path.getFullPath());
	}
	
	public void queueDirectoryList(final PathElement path) throws IOException
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
			communication.finish();
		}
		finally
		{
			lock.unlock();
		}
	}
}
