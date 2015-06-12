
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


package org.cnv.shr.gui;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;

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
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.ExplorerSyncIterator;
import org.cnv.shr.sync.FileFileSource;
import org.cnv.shr.sync.FileSource;
import org.cnv.shr.sync.LocalSynchronizer;
import org.cnv.shr.sync.RemoteFileSource;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.sync.SynchronizationTask;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;


public class PathTreeModel implements TreeModel
{
        private MachineViewer viewer;
    
	LinkedList<TreeModelListener> listeners = new LinkedList<>();
	RootDirectory rootDirectory;
	private PathTreeModelNode root;

	private RootSynchronizer synchronizer;
	ExplorerSyncIterator iterator;
	private FileSource rootSource;
	
	public PathTreeModel(MachineViewer viewer)
	{
		root = new PathTreeModelNode(null, this, new NoPath());
                this.viewer = viewer;
	}
	
	void closeConnections()
	{
		if (synchronizer != null)
		{
			synchronizer.quit();
		}
		iterator = null;
		rootSource = null;
		synchronizer = null;
	}
	
	public void setRoot(final RootDirectory newRoot)
	{
		final PathTreeModelNode oldroot = this.root;
		this.rootDirectory = newRoot;
		closeConnections();
		
		startRemoteSynchronizer(newRoot);
		this.root = new PathTreeModelNode(null, this, DbPaths.ROOT);
		try
		{
				iterator.queueSyncTask(rootSource, DbPaths.ROOT, this.root);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create remote synchronizer", e);
		}
		
		Services.userThreads.execute(synchronizer);
		this.root.expand();
		for (final TreeModelListener listener : listeners)
		{
			listener.treeStructureChanged(new TreeModelEvent(this, new Object[] {oldroot}));
		}
	}

	private void startRemoteSynchronizer(final RootDirectory newRoot)
	{
		try
		{
			iterator = new ExplorerSyncIterator(rootDirectory);
			if (rootDirectory.isLocal())
			{
        viewer.setSyncStatus("Browsing local files.");
				rootSource = new FileFileSource(new File(
						rootDirectory.getPathElement().getFsPath()),
						DbRoots.getIgnores((LocalDirectory) newRoot));
				synchronizer = new LocalSynchronizer((LocalDirectory) rootDirectory, iterator);
                                
			}
			else
			{
        viewer.setSyncStatus("Connecting...");
				final RemoteSynchronizerQueue createRemoteSynchronizer = Services.syncs.createRemoteSynchronizer((RemoteDirectory) rootDirectory);
				rootSource = new RemoteFileSource((RemoteDirectory) rootDirectory, createRemoteSynchronizer);
				iterator.setCloseable(createRemoteSynchronizer);
				synchronizer = new RemoteSynchronizer((RemoteDirectory) rootDirectory, iterator);
        viewer.setSyncStatus("Connected to remote.");
			}
                        
		}
		catch (final IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create remote synchronizer", e);
			closeConnections();
			setToNullSynchronizers(newRoot);
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
	
	
	
	
	
	
	
	
	
	
	
	
	


	private void setToNullSynchronizers(final RootDirectory newRoot)
	{
		viewer.setSyncStatus("Not connected, browsing cache.");
		iterator = new ExplorerSyncIterator(newRoot) {
			@Override
			public SynchronizationTask next()
			{ throw new RuntimeException("Don't call this."); }
			@Override
			public SynchronizationTask queueSyncTask(final FileSource file, final PathElement dbDir, final TaskListener listener) throws IOException
			{ return null; }
			@Override
			public void close() throws IOException {}
		};
		rootSource = new FileSource() {
			@Override
			public boolean stillExists() { return false; }
			@Override
			public FileSourceIterator listFiles() throws IOException { return FileSource.NULL_ITERATOR; }
			@Override
			public String getName() { throw new RuntimeException("Don't call this."); }
			@Override
			public boolean isDirectory() { throw new RuntimeException("Don't call this."); }
			@Override
			public boolean isFile() { throw new RuntimeException("Don't call this."); }
			@Override
			public String getCanonicalPath() { throw new RuntimeException("Don't call this."); }
			@Override
			public long getFileSize() { throw new RuntimeException("Don't call this."); }
			@Override
			public SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException { { throw new RuntimeException("Don't call this."); }}
		};
		synchronizer = new RootSynchronizer(newRoot, iterator) {
			@Override
			public void quit() {}
			@Override
			public void addListener(final SynchronizationListener listener) {}
			@Override
			public void removeListener(final SynchronizationListener listener) {}
			@Override
			public void run() {}
			@Override
			protected boolean updateFile(SharedFile file) {return false;}
		};
	}
}
