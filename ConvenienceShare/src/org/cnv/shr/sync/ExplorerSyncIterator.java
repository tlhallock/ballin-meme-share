
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

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.FileSource.FileSourceIterator;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;

public class ExplorerSyncIterator extends SyncrhonizationTaskIterator
{
	private RootDirectory root;
	
	private boolean quit;
	
	private LinkedBlockingDeque<MySyncTask> tasks = new LinkedBlockingDeque<>();
	
	public ExplorerSyncIterator(final RootDirectory remoteDirectory)
	{
		root = remoteDirectory;
	}

	@Override
	public SynchronizationTask next() throws IOException, InterruptedException
	{
		while (true)
		{
			if (quit)
				return null;

			MySyncTask t = tasks.pollFirst(10, TimeUnit.SECONDS);
			if (t == null)
				continue;

			if (!t.file.stillExists())
				continue;

			try (FileSourceIterator grandChildren = t.file.listFiles();)
			{
				final SynchronizationTask synchronizationTask = new SynchronizationTask(t.dbDir, grandChildren);
				synchronizationTask.addListener(t.listener);
				return synchronizationTask;
			}
		}
	}

	public void queueSyncTask(final FileSource file, final PathElement dbDir, final TaskListener listener)
	{
		tasks.offerLast(new MySyncTask(file, dbDir, listener));
	}
	
	@Override
	public void close() throws IOException
	{
		quit = true;
		super.close();
	}
	
	class MySyncTask
	{
		private FileSource file;
		private PathElement dbDir;
		private TaskListener listener;
		
		public MySyncTask(FileSource file, PathElement dbDir, TaskListener listener)
		{
			this.file = file;
			this.dbDir = dbDir;
			this.listener = listener;
		}
	}
}
