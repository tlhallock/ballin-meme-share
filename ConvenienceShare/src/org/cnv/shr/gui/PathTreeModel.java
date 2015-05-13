package org.cnv.shr.gui;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.ExplorerSyncIterator;
import org.cnv.shr.sync.FileSource;
import org.cnv.shr.sync.Pair;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RootSynchronizer;
import org.cnv.shr.sync.SynchronizationTask;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;

public class PathTreeModel implements TreeModel
{
	private LinkedList<TreeModelListener> listeners = new LinkedList<>();
	private RootDirectory rootDirectory;
	private Node root = new Node(new NoPath());

	private RootSynchronizer synchronizer;
	private ExplorerSyncIterator iterator;
	private FileSource rootSource;

	private void closeConnections()
	{
		if (synchronizer == null)
		{
			return;
		}
		try
		{
			synchronizer.close();
			iterator.close();
			rootSource.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void setRoot(RootDirectory newRoot) throws IOException
	{
		closeConnections();
		iterator = new ExplorerSyncIterator(rootDirectory);
		synchronizer = new RemoteSynchronizer(rootDirectory, iterator);
		rootSource = iterator.getInitialFileSource();
		// actually have to kick the synchronizer off...
		
		RootDirectory oldroot = this.rootDirectory;
		this.rootDirectory = newRoot;
		this.root = new Node(DbPaths.ROOT);
		this.root.task = iterator.getSyncTask(rootSource, DbPaths.ROOT, this.root);
		this.root.expand();
		if (oldroot == null)
		{
			return;
		}
		for (TreeModelListener listener : listeners)
		{
			listener.treeStructureChanged(new TreeModelEvent(this, new Object[] {oldroot}));
		}
	}
	
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
	public Object getChild(Object parent, int index)
	{
		Node n = (Node) parent;
		if (n.children == null)
		{
			n.expand();
		}
		return ((Node) parent).children[index];
	}

	@Override
	public int getChildCount(Object parent)
	{
		Node n = (Node) parent;
		if (n.children == null)
		{
			n.expand();
		}
		return ((Node) parent).children.length;
	}

	@Override
	public boolean isLeaf(Object node)
	{
		return ((Node) node).isLeaf();
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue)
	{
		System.out.println("Changed");
	}

	@Override
	public int getIndexOfChild(Object parent, Object child)
	{
		Node n = (Node) parent;
		if (n.children == null)
		{
			n.expand();
		}
		return n.getIndexOfChild((Node) child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener l)
	{
		listeners.add(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l)
	{
		listeners.remove(l);
	}
	
	public LinkedList<SharedFile> getList(Node n)
	{
		LinkedList<SharedFile> list = new LinkedList<>();
		
		if (n.children == null)
		{
			return list;
		}
		
		for (Node child : n.children)
		{
			SharedFile file = child.getFile();
			if (file != null)
			{
				list.add(file);
			}
		}
		return list;
	}

	public RootDirectory getRootDirectory()
	{
		return rootDirectory;
	}

	class Node implements TaskListener
	{
		PathElement element;
		Node[] children;
		FileSource[] childSources;
		
		SynchronizationTask task;
		
		public Node(PathElement element)
		{
			this.element = element;
			this.children = null;
		}
		
		public boolean isLeaf()
		{
			// todo: fixme
			if (rootDirectory == null)
			{
				return true;
			}
			return getFile() != null;
		}

		public int getIndexOfChild(Node child)
		{
			for (int i = 0; i < children.length; i++)
			{
				if (children[i].equals(child))
				{
					return i;
				}
			}
			return 0;
		}

		public String toString()
		{
			return element.getUnbrokenName();
		}
		
		public int hashCode()
		{
			if (element == null)
			{
				return 1;
			}
			return element.getFullPath().hashCode();
		}
		
		public boolean equals(Node n)
		{
			return element.getFullPath().equals(n.element.getFullPath());
		}
		
		private synchronized void expand()
		{
			System.out.println("Expanding " + element.getFullPath());
			System.out.println("Expanding " + element.getId());
			if (rootDirectory == null)
			{
				children = new Node[0];
				return;
			}
			LinkedList<PathElement> list = element.list(rootDirectory);
			// Should clean this one up...
			for (PathElement e : list)
			{
				if (e.getFullPath().equals("/"))
				{
					list.remove(e);
					break;
				}
			}
			children = new Node[list.size()];
			int ndx = 0;
			for (PathElement e : list)
			{
				children[ndx] = new Node(e);
				
				if (childSources[ndx] != null)
				{
					children[ndx].task = iterator.getSyncTask(childSources[ndx], element, children[ndx]);
				}
				ndx++;
			}
		}

		@Override
		public synchronized void syncCompleted()
		{
			boolean alreadyExpanded = children != null;
			for (Pair<? extends FileSource> p : task.getSynchronizationResults())
			{
				Node child = null;
				
				if (!alreadyExpanded)
				{
					childSources[ndx] = p.getSource();
					continue;
				}
				
				children[ndx].source = p.getSource();
				children[ndx].task = iterator.getSyncTask(childSources[ndx], element, children[ndx]);

				for (TreeModelListener listener : listeners)
				{
					// listener.treeStructureChanged(new TreeModelEvent(this,
					// new Object[] {oldroot}));
				}
			}
		}

		public SharedFile getFile()
		{
			return DbFiles.getFile(rootDirectory, element);
		}
	}
	
	public static class NoPath extends PathElement
	{
		public NoPath()
		{
			super(null);
		}

		public String toString()
		{
			return "No root selected";
		}
		
		public String getFullPath()
		{
			return "No root selected";
		}
		
		public String getUnbrokenName()
		{
			return "No root selected.";
		}
		
		// These are not actually needed...
		public int hashCode()
		{
			return 1;
		}
		
		public boolean equals(PathElement e)
		{
			return e instanceof NoPath;
		}
	}
}
