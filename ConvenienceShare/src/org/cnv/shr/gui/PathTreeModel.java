package org.cnv.shr.gui;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.sync.ExplorerSyncIterator;
import org.cnv.shr.sync.FileFileSource;
import org.cnv.shr.sync.FileSource;
import org.cnv.shr.sync.LocalSynchronizer;
import org.cnv.shr.sync.RemoteFileSource;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.sync.RootSynchronizer;


public class PathTreeModel implements TreeModel
{
	LinkedList<TreeModelListener> listeners = new LinkedList<>();
	RootDirectory rootDirectory;
	private PathTreeModelNode root;

	private RootSynchronizer synchronizer;
	ExplorerSyncIterator iterator;
	private FileSource rootSource;
	
	public PathTreeModel()
	{
		root = new PathTreeModelNode(null, this, new NoPath());
	}
	
	void closeConnections()
	{
		if (synchronizer != null)
		{
			synchronizer.quit();
		}
	}
	
	public void setRoot(final RootDirectory newRoot)
	{
		final RootDirectory oldroot = this.rootDirectory;
		this.rootDirectory = newRoot;
		closeConnections();
		try
		{
			iterator = new ExplorerSyncIterator(rootDirectory);
			if (rootDirectory.isLocal())
			{
				rootSource = new FileFileSource(new File(rootDirectory.getPathElement().getFullPath()),
						DbRoots.getIgnores((LocalDirectory) newRoot));
				synchronizer = new LocalSynchronizer((LocalDirectory) rootDirectory, iterator);
			}
			else
			{
				final RemoteSynchronizerQueue createRemoteSynchronizer = Services.syncs.createRemoteSynchronizer((RemoteDirectory) rootDirectory);
				rootSource = new RemoteFileSource((RemoteDirectory) rootDirectory, createRemoteSynchronizer);
				iterator.setCloseable(createRemoteSynchronizer);
				synchronizer = new RemoteSynchronizer((RemoteDirectory) rootDirectory, iterator);
			}
			
			this.root = new PathTreeModelNode(null, this, DbPaths.ROOT);
			iterator.queueSyncTask(rootSource, DbPaths.ROOT, this.root);
			Services.userThreads.execute(synchronizer);
			this.root.expand();
		}
		catch (final IOException e)
		{
			Services.logger.print(e);
		}
		if (oldroot == null)
		{
			return;
		}
		for (final TreeModelListener listener : listeners)
		{
			listener.treeStructureChanged(new TreeModelEvent(this, new Object[] {oldroot}));
		}
	}
	
	@Override
	public void finalize()
	{
		closeConnections();
	}
	
	@Override
	public Object getRoot()
	{
		return root;
	}

	@Override
	public Object getChild(final Object parent, final int index)
	{
		final PathTreeModelNode n = (PathTreeModelNode) parent;
		if (n.children == null)
		{
			n.expand();
		}
		return ((PathTreeModelNode) parent).children[index];
	}

	@Override
	public int getChildCount(final Object parent)
	{
		final PathTreeModelNode n = (PathTreeModelNode) parent;
		if (n.children == null)
		{
			n.expand();
		}
		return ((PathTreeModelNode) parent).children.length;
	}

	@Override
	public boolean isLeaf(final Object node)
	{
		return ((PathTreeModelNode) node).isLeaf();
	}

	@Override
	public void valueForPathChanged(final TreePath path, final Object newValue)
	{
		System.out.println("Changed");
	}

	@Override
	public int getIndexOfChild(final Object parent, final Object child)
	{
		final PathTreeModelNode n = (PathTreeModelNode) parent;
		if (n.children == null)
		{
			n.expand();
		}
		return n.getIndexOfChild((PathTreeModelNode) child);
	}

	@Override
	public void addTreeModelListener(final TreeModelListener l)
	{
		listeners.add(l);
	}

	@Override
	public void removeTreeModelListener(final TreeModelListener l)
	{
		listeners.remove(l);
	}
	
	public RootDirectory getRootDirectory()
	{
		return rootDirectory;
	}
	
	public static class NoPath extends PathElement
	{
		public NoPath()
		{
			super(null);
		}
		
		@Override
		public Long getId()
		{
			return -1L;
		}

		@Override
		public String toString()
		{
			return "No root selected";
		}
		
		@Override
		public String getFullPath()
		{
			return "No root selected";
		}
		
		@Override
		public String getUnbrokenName()
		{
			return "No root selected.";
		}
		
		// These are not actually needed...
		@Override
		public int hashCode()
		{
			return 1;
		}
		
		@Override
		public boolean equals(final PathElement e)
		{
			return e instanceof NoPath;
		}
	}
}
