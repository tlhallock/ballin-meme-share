package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.lcl.RemoteSynchronizers;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class DirectoryList extends Message
{
	String name;
	String currentPath;
	LinkedList<String> subDirs = new LinkedList<>();
	LinkedList<Child> children = new LinkedList<>();

	public DirectoryList(LocalDirectory localByName, PathElement pathElement)
	{
		name = localByName.getName();
		currentPath = pathElement.getFullPath();
		for (PathElement element : pathElement.list(localByName))
		{
			LocalFile local = DbFiles.getFile(localByName, element);
			if (local == null)
			{
				subDirs.add(element.getUnbrokenName());
			}
			else
			{
				children.add(new Child(local));
			}
		}
	}

	protected DirectoryList(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		name = ByteReader.readString(bytes);
		currentPath = ByteReader.readString(bytes);
		int numDirs = ByteReader.readInt(bytes);
		for (int i=0;i<numDirs;i++)
		{
			subDirs.add(ByteReader.readString(bytes));
		}
		int numFiles = ByteReader.readInt(bytes);
		for (int i=0;i<numFiles;i++)
		{
			children.add(new Child(bytes));
		}
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(name);
		buffer.append(currentPath);
		buffer.append(subDirs.size());
		for (String sub : subDirs)
		{
			buffer.append(sub);
		}
		buffer.append(children.size());
		for (Child c : children)
		{
			c.write(buffer);
		}
	}

	@Override
	protected int getType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		RemoteSynchronizers.RemoteSynchronizer sync = Services.syncs.getSynchronizer(connection, getRoot());
		if (sync == null)
		{
			Services.logger.logStream.println("Lost synchronizer?");
			return;
		}
		sync.receiveList(this);
	}

	public String getCurrentPath()
	{
		return getPath().getFullPath();
	}
	
	RemoteDirectory rootCache;
	RemoteDirectory getRoot()
	{
		if (rootCache != null)
		{
			return rootCache;
		}
		return rootCache = DbRoots.getRemote(getMachine(), name);
	}
	
	PathElement elemCache;
	PathElement getPath()
	{
		if (elemCache != null)
		{
			return elemCache;
		}
		return elemCache = DbPaths.getRelPathElement(getRoot(), currentPath);
	}
	
	public class Child
	{
		String name;
		long size;
		String checksum;
		String tags;
		long lastModified;
		
		Child(LocalFile l)
		{
			this.name = l.getPath().getUnbrokenName();
			this.size = l.getFileSize();
			this.checksum = l.getChecksum() == null ? "" : l.getChecksum();
			this.tags = l.getTags();
			this.lastModified = l.getLastUpdated();
		}
		
		Child(InputStream bytes) throws IOException
		{
			name = ByteReader.readString(bytes);
			size = ByteReader.readLong(bytes);
			checksum = ByteReader.readString(bytes);
			tags = ByteReader.readString(bytes);
			lastModified = ByteReader.readLong(bytes);
		}
		
		public void write(ByteListBuffer buffer)
		{
			buffer.append(name);
			buffer.append(size);
			buffer.append(checksum);
			buffer.append(tags);
			buffer.append(lastModified);
		}
		
		public RemoteFile create() 
		{
			PathElement pathElement = DbPaths.getRelPathElement(getRoot(), name);
			return new RemoteFile(getRoot(), pathElement,
					size, checksum, tags, lastModified);
		}
	}

	public LinkedList<Child> getChildren()
	{
		return children;
	}

	public LinkedList<String> getSubDirs()
	{
		return subDirs;
	}
}
