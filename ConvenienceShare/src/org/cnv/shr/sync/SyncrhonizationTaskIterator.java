package org.cnv.shr.sync;

import java.io.Closeable;
import java.io.IOException;


public abstract class SyncrhonizationTaskIterator implements Closeable
{
	// A little bit ugly, but I think this is the best spot for the closeable.
	protected Closeable connection;
	
	public abstract SynchronizationTask next();

	public void setCloseable(final Closeable closeable)
	{
		this.connection = closeable;
	}

	@Override
	public void close() throws IOException
	{
		if (connection != null)
		{
			connection.close();
		}
	}
}
