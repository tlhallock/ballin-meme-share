
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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
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
	
	private long lastSync;
	private boolean syncFully;

	public PathTreeModelNode(final PathTreeModelNode parent, 
													 final PathTreeModel model,
													 final PathElement element,
													 boolean syncFully)
	{
		Objects.requireNonNull(element, "Path must not be null.");
		
		this.parent = parent;
		this.model = model;
		this.element = element;
		this.children = null;
		this.syncFully = syncFully;
	}
	
	public PathElement getPathElement()
	{
		return element;
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
		if (element.getId() == 0)
		{
			RootDirectory rootDir = model.getRootDirectory();
			return rootDir == null ? "No directory chosen" : rootDir.getName();
		}
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
		RootDirectory rootDir = model.getRootDirectory();
		if (rootDir == null)
		{
			return null;
		}
		return DbFiles.getFile(rootDir, element);
	}
	
	public synchronized void syncFully(RootDirectory rootDir)
	{
		syncFully(rootDir, true);
	}
	public synchronized void syncFully(RootDirectory root, boolean val)
	{
		lastSync = 0;
		syncFully = val;
		if (children != null)
		{
			for (PathTreeModelNode node : children)
			{
				node.syncFully(root, val);
			}
		}
		if (sourceChildren != null)
		{
			setToSource(root);
		}
	}
	
	@Override
	public synchronized void syncCompleted(final Pair[] pairs)
	{
		this.sourceChildren = pairs;
		if (syncFully || children != null)
		{
			RootDirectory rootDir = model.getRootDirectory();
			setToSource(rootDir);
		}
	}
	
    void ensureExpanded() {
        if (!isLeaf() && children == null) expand();
    }
	synchronized void expand()
	{
		LogWrapper.getLogger().info("Expanding " + element.getFullPath());
		RootDirectory rootDir = model.getRootDirectory();
		if (rootDir == null)
		{
			children = new PathTreeModelNode[0];
			return;
		}

		if (sourceChildren != null)
		{
			setToSource(rootDir);
		}
		else
		{
			setToDb(rootDir);
		}
	}

	private synchronized void setToDb(RootDirectory rootDir)
	{
		// List from database...
		final LinkedList<PathElement> list = element.list(rootDir);
		// Should clean this one up...
		for (final PathElement e : list)
		{
			if (e.isAbsolute())
			{
				list.remove(e);
				break;
			}
		}
		children = new PathTreeModelNode[list.size()];
		int ndx = 0;
		for (final PathElement e : list)
		{
			children[ndx++] = new PathTreeModelNode(this, model, e, syncFully);
		}
	}
	
	public synchronized void setToSource(RootDirectory rootDir)
	{
		long now = System.currentTimeMillis();
		if (children != null && lastSync + 5 * 60 * 1000 > now)
		{
			return;
		}
		lastSync = now;
		
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
			String unbrokenName = pathElement.getUnbrokenName();
			if (pathElement.isAbsolute())
			{
				continue;
			}
			accountedFor.add(unbrokenName);
			PathTreeModelNode node = new PathTreeModelNode(this, model, pathElement, syncFully);
			allChildren.add(node);
			
			model.getIterator().queueSyncTask(p.getSource(), pathElement, node);
		}
		// add sub files...
		for (final PathElement childElement : element.list(rootDir))
		{
			if (accountedFor.contains(childElement.getUnbrokenName()))
			{
				continue;
			}
			final SharedFile file = DbFiles.getFile(rootDir, childElement);
			if (file == null)
			{
				continue;
			}
			allChildren.add(new PathTreeModelNode(this, model, childElement, syncFully));
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
	
	public List<SharedFile> getFileList(boolean recursive)
	{
		LogWrapper.getLogger().info("Finding children of \"" + element.getFullPath() + "\"");
		RootDirectory rootDir = model.getRootDirectory();
		if (rootDir == null)
		{
			return Collections.emptyList();
		}
		
		if (isLeaf())
		{
			return Collections.singletonList(getFile());
		}
		
		final LinkedList<SharedFile> list = new LinkedList<>();

		if (recursive)
		{
			element.getFilesList(model.getRootDirectory(), list);
			return list;
		}
		
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
