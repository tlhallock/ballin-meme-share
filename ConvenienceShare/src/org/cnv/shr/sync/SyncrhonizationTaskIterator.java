package org.cnv.shr.sync;

import java.io.Closeable;

public interface SyncrhonizationTaskIterator extends Closeable
{
	public SynchronizationTask next();
//new ConsecutiveDirectorySyncIterator(remoteDirectory, f)
}
