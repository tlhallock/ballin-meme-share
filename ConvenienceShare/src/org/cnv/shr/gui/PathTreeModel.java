
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

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.sync.SynchronizationTask;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;
import org.cnv.shr.sync.SyncrhonizationTaskIterator;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;


public class PathTreeModel extends TimerTask implements TreeModel, Closeable, SynchronizationListener, WindowListener
{
  static final long INACTIVITY_DELAY = 10 * 60 * 1000;

	private MachineViewer viewer;
    
	LinkedList<TreeModelListener> listeners = new LinkedList<>();
	private PathTreeModelNode root;

	private RootSynchronizer synchronizer;
	private ExplorerSyncIterator iterator;
	private FileSource rootSource;
	
	private static ExecutorService syncRunnerService = Executors.newCachedThreadPool();
	private PathTreeModelRunnable syncRunner;
	
	private boolean active;
	
	public PathTreeModel(MachineViewer viewer)
	{
		root = new PathTreeModelNode(null, this, new NoPath(), false);
    this.viewer = viewer;
    syncRunner = new PathTreeModelRunnable();
    viewer.addWindowListener(syncRunner);
    syncRunnerService.execute(syncRunner);
	}
	
	void shutDown()
	{
		close();
		cancel();
		syncRunner.quit();
	}
	
	public synchronized void close()
	{
		if (synchronizer != null)
		{
			try
			{
				synchronizer.quit();
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to quit synchronizer", e);
			}
		}
		iterator = null;
		rootSource = null;
		synchronizer = null;
		syncRunner.setSynchronizer(null);
	}

	public void resetRoot()
	{
		setRoot(viewer.getRootDirectory());
	}
	
	ExplorerSyncIterator getIterator()
	{
//		if (iterator == null || iterator instanceof NullIterator)
//		{
//			startRemoteSynchronizer(viewer.getRootDirectory());
//		}
		return iterator;
	}
	
	public void setRoot(final RootDirectory newRoot)
	{
		final PathTreeModelNode oldroot = this.root;
		close();
		
		startRemoteSynchronizer(newRoot);
		this.root = new PathTreeModelNode(null, this, DbPaths.ROOT, false);
		iterator.queueSyncTask(rootSource, DbPaths.ROOT, this.root);
		
		this.root.expand();
		for (final TreeModelListener listener : listeners)
		{
			listener.treeStructureChanged(new TreeModelEvent(this, new Object[] {oldroot}));
		}
	}

	private synchronized void startRemoteSynchronizer(final RootDirectory newRoot)
	{
		try
		{
			LogWrapper.getLogger().info("Starting local explorer synchronizer");
			RootDirectory rootDirectory = viewer.getRootDirectory();
			iterator = new ExplorerSyncIterator(rootDirectory);
			if (rootDirectory.isLocal())
			{
        viewer.setSyncStatus(Color.GREEN, Color.BLACK, "Browsing local files.");
				rootSource = new FileFileSource(new File(
						rootDirectory.getPathElement().getFsPath()),
						DbRoots.getIgnores((LocalDirectory) newRoot));
				synchronizer = new LocalSynchronizer((LocalDirectory) rootDirectory, iterator);
                                
			}
			else
			{
				LogWrapper.getLogger().info("Starting remote explorer synchronizer");
        viewer.setSyncStatus(Color.YELLOW, Color.BLACK, "Connecting...");
				final RemoteSynchronizerQueue createRemoteSynchronizer = Services.syncs.createRemoteSynchronizer(viewer, (RemoteDirectory) rootDirectory);
				rootSource = new RemoteFileSource((RemoteDirectory) rootDirectory, createRemoteSynchronizer);
				iterator.setCloseable(createRemoteSynchronizer);
				synchronizer = new RemoteSynchronizer((RemoteDirectory) rootDirectory, iterator);
				synchronizer.addListener(this);
        viewer.setSyncStatus(Color.GREEN, Color.BLACK, "Connected to remote.");
			}
		}
		catch (final IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create remote synchronizer", e);
			close();
			setToNullSynchronizers(newRoot);
		}
		finally
		{
			active = true;
			syncRunner.setSynchronizer(synchronizer);
		}
	}
	
	@Override
	public void finalize()
	{
		close();
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
	public void valueForPathChanged(final TreePath path, final Object newValue) {}
	
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
		return viewer.getRootDirectory();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	public static class NoPath extends PathElement
	{
		public NoPath()
		{
			super(-1L);
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
		viewer.setSyncStatus(Color.RED, Color.WHITE, "Not connected, browsing cache.");
		iterator = new NullIterator(newRoot);
		rootSource = new FileSourceImplementation();
		synchronizer = new RootSynchronizerExtension(newRoot, iterator);
	}

	@Override
	public void beganDirectory(String str) { active = true; }
	@Override
	public void fileAdded(SharedFile f)    { active = true; }
	@Override
	public void fileRemoved(SharedFile f)  { active = true; }
	@Override
	public void fileUpdated(SharedFile f)  { active = true; }
	@Override
	public void syncDone(RootSynchronizer sync)
	{
		if (synchronizer == null || !synchronizer.equals(sync))
		{
			return;
		}
		viewer.setSyncStatus(Color.RED, Color.WHITE, "Connection closed.");
	}
	
	public void run()
	{
		if (!active && iterator != null && !(iterator instanceof NullIterator))
		{
			LogWrapper.getLogger().info("Closing synchronizer due to inactivity.");
			close();
		}
		active = false;
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		shutDown();
	}
	@Override
	public void windowOpened(WindowEvent e) {}
	@Override
	public void windowClosing(WindowEvent e) {}
	@Override
	public void windowIconified(WindowEvent e) {}
	@Override
	public void windowDeiconified(WindowEvent e) {}
	@Override
	public void windowActivated(WindowEvent e) {}
	@Override
	public void windowDeactivated(WindowEvent e) {}
	

	private static final class RootSynchronizerExtension extends RootSynchronizer
	{
		private RootSynchronizerExtension(RootDirectory remoteDirectory, SyncrhonizationTaskIterator iterator)
		{
			super(remoteDirectory, iterator);
		}

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
	}
	private static final class FileSourceImplementation implements FileSource
	{
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
	}
	private static final class NullIterator extends ExplorerSyncIterator
	{
		private NullIterator(RootDirectory remoteDirectory)
		{
			super(remoteDirectory);
		}

		@Override
		public SynchronizationTask next()
		{ throw new RuntimeException("Don't call this."); }

		@Override
		public void queueSyncTask(final FileSource file, final PathElement dbDir, final TaskListener listener) {}

		@Override
		public void close() throws IOException {}
	}
}
