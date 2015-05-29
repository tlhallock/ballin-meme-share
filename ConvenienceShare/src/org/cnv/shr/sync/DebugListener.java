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
