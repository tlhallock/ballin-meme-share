
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

import java.util.TimerTask;

import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.util.LogWrapper;

public class DebugListener extends TimerTask implements SynchronizationListener
{
	protected int filesAdded;
	protected long filesRefresh;
	protected long filesRemoved;
	protected long bytesAdded;
	protected String currentFile;
	
	private RootDirectory local;
	
	public DebugListener(RootDirectory local)
	{
		this.local = local;
	}
	
	@Override
	public void fileAdded(SharedFile f)
	{
		filesAdded++;
		bytesAdded += f.getFileSize();
		changed();
	}
	
	@Override
	public void fileRemoved(SharedFile f)
	{
		filesRemoved++;
		bytesAdded -= f.getFileSize();
		changed();
	}
	
	@Override
	public void fileUpdated(SharedFile f)
	{
		filesRefresh++;
		// should set bytes added...
		changed();
	}

	@Override
	public void beganDirectory(String str)
	{
		currentFile = str;
	}

	public static long DEBUG_REPEAT = 5000;
	private long lastDebug = System.currentTimeMillis();
	@Override
	public void run()
	{
		long now = System.currentTimeMillis();
		double seconds = (now - lastDebug) / 1000;

		synchronized (LogWrapper.getLogger())
		{
			LogWrapper.getLogger().info("-------------------------------------------------------");
			LogWrapper.getLogger().info("Synchronizing: " + local.getPathElement().getFullPath());
			LogWrapper.getLogger().info("Current file: " + currentFile);
			LogWrapper.getLogger().info("File refresh rate: " + filesRefresh / seconds + "/s");
			LogWrapper.getLogger().info("File add rate: " + filesAdded / seconds + "/s");
			LogWrapper.getLogger().info("File remove rate: " + filesRemoved / seconds + "/s");
			LogWrapper.getLogger().info("-------------------------------------------------------");
		}
		lastDebug = now;
		filesRefresh = 0;
		filesAdded = 0;
		filesRemoved = 0;
		bytesAdded = 0;
	}
	
	protected void changed() {}
}
