package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.RemoteSynchronizers;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DirectoryList extends Message
{
	public static int TYPE = 19;
	
	private String name;
	private String currentPath;
	private LinkedList<String> subDirs = new LinkedList<>();
	private LinkedList<Child> children = new LinkedList<>();

	public DirectoryList(InputStream input) throws IOException
	{
		super(input);
	}	

	public DirectoryList(LocalDirectory localByName, PathElement pathElement)
	{
		name = localByName.getName();
		currentPath = pathElement.getFullPath();
		for (PathElement element : pathElement.list(localByName))
		{
			SharedFile local = DbFiles.getFile(localByName, element);
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
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		System.out.println("[" + name + ":" + currentPath + "]:");
		for (String subdir : subDirs)
		{
			builder.append(subdir).append(":");
		}
		builder.append("=");
		for (Child c : children)
		{
			builder.append(c.name).append(":");
		}
		
		return builder.toString();
	}

	@Override
	public void parse(InputStream bytes) throws IOException
	{
		name = ByteReader.readString(bytes);
		currentPath = ByteReader.readString(bytes);
		int numDirs = ByteReader.readInt(bytes);
		for (int i = 0; i < numDirs; i++)
		{
			subDirs.add(ByteReader.readString(bytes));
		}
		int numFiles = ByteReader.readInt(bytes);
		for (int i = 0; i < numFiles; i++)
		{
			children.add(new Child(bytes));
		}
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
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
	public void perform(Communication connection) throws Exception
	{
		getRoot(connection.getMachine());
		RemoteSynchronizers.RemoteSynchronizerQueue sync = Services.syncs.getSynchronizer(connection, getRoot());
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
	RemoteDirectory getRoot(Machine machine)
	{
		if (rootCache != null)
		{
			return rootCache;
		}
		return rootCache = (RemoteDirectory) DbRoots.getRoot(machine, name);
	}

	private RemoteDirectory getRoot()
	{
		return rootCache;
	}
	
	PathElement elemCache;
	PathElement getPath()
	{
		if (elemCache != null)
		{
			return elemCache;
		}
		return elemCache = DbPaths.getPathElement(currentPath);
	}
	
	public class Child
	{
		private String name;
		private long size;
		private String checksum;
		private String tags;
		private long lastModified;
		
		Child(SharedFile l)
		{
			this.name = l.getPath().getUnbrokenName();
			this.size = l.getFileSize();
			this.checksum = l.getChecksum() == null ? "" : l.getChecksum();
			this.tags = l.getTags();
			this.lastModified = l.getLastUpdated();
		}
		
		Child (InputStream bytes) throws IOException
		{
			name = ByteReader.readString(bytes);
			size = ByteReader.readLong(bytes);
			checksum = ByteReader.readString(bytes);
			tags = ByteReader.readString(bytes);
			lastModified = ByteReader.readLong(bytes);
		}
		
		public void write(AbstractByteWriter buffer) throws IOException
		{
			buffer.append(name);
			buffer.append(size);
			buffer.append(checksum);
			buffer.append(tags == null ? "" : tags);
			buffer.append(lastModified);
		}
		
		public RemoteFile create() 
		{
			PathElement pathElement = DbPaths.getPathElement(getPath(), name);
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

	@Override
	protected int getType()
	{
		return TYPE;
	}
}
