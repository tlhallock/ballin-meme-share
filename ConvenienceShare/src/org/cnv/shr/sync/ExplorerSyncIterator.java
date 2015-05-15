package org.cnv.shr.sync;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;

public class ExplorerSyncIterator implements SyncrhonizationTaskIterator
{
	private RootDirectory root;
//	private FileSource source;
	
	// There is a better class for this...
	private LinkedList<SynchronizationTask> tasks = new LinkedList<>();
	
	public ExplorerSyncIterator(RootDirectory remoteDirectory) throws IOException
	{
		root = remoteDirectory;
	}

	@Override
	public SynchronizationTask next()
	{
		return tasks.getLast();
	}

	private void queue(SynchronizationTask synchronizationTask)
	{
		tasks.addLast(synchronizationTask);
	}

	@Override
	public void close() throws IOException
	{
//		source.close();
	}

	public FileSource getInitialFileSource() throws UnknownHostException, IOException
	{
		if (root.isLocal())
		{
			return new FileFileSource(new File(root.getPathElement().getFullPath()));
		}
		else
		{
			return new RemoteFileSource((RemoteDirectory) root, true);
		}
	}
	
	public SynchronizationTask getSyncTask(FileSource file, PathElement dbDir, TaskListener listener)
	{
		if (!file.stillExists())
		{
			return null;
		}
		Iterator<FileSource> grandChildren;
		try
		{
			grandChildren = file.listFiles();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
			return null;
		}
		if (grandChildren == null)
		{
			return null;
		}
		SynchronizationTask synchronizationTask = new SynchronizationTask(dbDir, root, grandChildren);
		synchronizationTask.addListener(listener);
		queue(synchronizationTask);
		return synchronizationTask;
	}
}
