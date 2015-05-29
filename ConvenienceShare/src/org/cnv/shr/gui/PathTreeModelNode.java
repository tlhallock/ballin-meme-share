package org.cnv.shr.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.Pair;
import org.cnv.shr.sync.SynchronizationTask.TaskListener;
import org.cnv.shr.util.LogWrapper;

public class PathTreeModelNode implements TaskListener
{
	private static final PathTreeModelNode[] DUMMY = new PathTreeModelNode[0];
	
	private PathTreeModel model;
	private PathTreeModelNode parent;
	
	private PathElement element;
	private Pair[] sourceChildren;
	
	PathTreeModelNode[] children;

	public PathTreeModelNode(final PathTreeModelNode parent, final PathTreeModel model, final PathElement element)
	{
		this.parent = parent;
		this.model = model;
		if (element == null)
		{
			throw new NullPointerException("Path must not be null.");
		}
		this.element = element;
		this.children = null;
	}
	
	private PathTreeModelNode[] getPath()
	{
		final LinkedList<PathTreeModelNode> list = new LinkedList<>();
		PathTreeModelNode n = this;
		while (n != null)
		{
			list.add(n);
			n = n.parent;
		}
		return list.toArray(DUMMY);
	}
	
	public boolean isLeaf()
	{
		if (model.rootDirectory == null)
		{
			return true;
		}
		return getFile() != null;
	}

	public int getIndexOfChild(final PathTreeModelNode child)
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

	@Override
	public String toString()
	{
		return element.getUnbrokenName();
	}
	
	@Override
	public int hashCode()
	{
		if (element == null)
		{
			return 1;
		}
		return element.getFullPath().hashCode();
	}
	
	public boolean equals(final PathTreeModelNode n)
	{
		return element.getFullPath().equals(n.element.getFullPath());
	}

	public SharedFile getFile()
	{
		return DbFiles.getFile(model.rootDirectory, element);
	}
	
	@Override
	public synchronized void syncCompleted(final Pair[] pairs)
	{
		this.sourceChildren = pairs;
		if (children != null)
		{
			setToSource();
		}
	}
	
    void ensureExpanded() {
        if (!isLeaf() && children == null) expand();
    }
	synchronized void expand()
	{
		System.out.println("Expanding " + element.getFullPath());
		if (model.rootDirectory == null)
		{
			children = new PathTreeModelNode[0];
			return;
		}

		if (sourceChildren != null)
		{
			setToSource();
		}
		else
		{
			setToDb();
		}
	}

	private synchronized void setToDb()
	{
		// List from database...
		final LinkedList<PathElement> list = element.list(model.rootDirectory);
		// Should clean this one up...
		for (final PathElement e : list)
		{
			if (e.getFullPath().equals("/"))
			{
				list.remove(e);
				break;
			}
		}
		children = new PathTreeModelNode[list.size()];
		int ndx = 0;
		for (final PathElement e : list)
		{
			children[ndx++] = new PathTreeModelNode(this, model, e);
		}
	}
	
	public synchronized void setToSource()
	{
		TreeModelEvent event = null;
		if (children != null)
		{
			event = new TreeModelEvent(this, getPath());
		}
		final LinkedList<PathTreeModelNode> allChildren = new LinkedList<>();
		final HashSet<String> accountedFor = new HashSet<>();
		// add sub directories first...
		for (final Pair p : sourceChildren)
		{
			final PathElement pathElement = p.getPathElement();
			accountedFor.add(pathElement.getUnbrokenName());
			PathTreeModelNode node = new PathTreeModelNode(this, model, pathElement);
			allChildren.add(node);
			try
			{
				model.iterator.queueSyncTask(p.getSource(), pathElement, node);
			}
			catch (final IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to create sync task.", e);
			}
		}
		// add sub files...
		for (final PathElement childElement : element.list(model.rootDirectory))
		{
			if (accountedFor.contains(childElement.getUnbrokenName()))
			{
				continue;
			}
			final SharedFile file = DbFiles.getFile(model.rootDirectory, childElement);
			if (file == null)
			{
				continue;
			}
			allChildren.add(new PathTreeModelNode(this, model, childElement));
		}
		children = allChildren.toArray(DUMMY);
		if (event == null)
		{
			return;
		}
		for (final TreeModelListener listener : model.listeners)
		{
			listener.treeStructureChanged(event);
		}
	}
	
	public List<SharedFile> getFileList()
	{
		if (isLeaf())
		{
			return Collections.singletonList(getFile());
		}
		
		final LinkedList<SharedFile> list = new LinkedList<>();
		
		if (children == null)
		{
			return list;
		}
		
		for (final PathTreeModelNode child : children)
		{
			final SharedFile file = child.getFile();
			if (file != null)
			{
				list.add(file);
			}
		}
		return list;
	}
}
