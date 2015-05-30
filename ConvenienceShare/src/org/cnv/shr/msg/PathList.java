package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class PathList extends Message
{
	public static int TYPE = 19;
	
	private String name;
	private String currentPath;
	private LinkedList<String> subDirs = new LinkedList<>();
	private LinkedList<Child> children = new LinkedList<>();

	public PathList(final InputStream input) throws IOException
	{
		super(input);
	}	

	public PathList(final LocalDirectory localByName, final PathElement pathElement)
	{
		name = localByName.getName();
		currentPath = pathElement.getFullPath();
		for (final PathElement element : pathElement.list(localByName))
		{
			final SharedFile local = DbFiles.getFile(localByName, element);
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
	
	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();
		
		System.out.println("[machine=" + name + "   currentpath=" + currentPath + "]:");
		builder.append("Subdirectories:\n");
		for (final String subdir : subDirs)
		{
			builder.append('\t').append(subdir).append('\n');
		}
		builder.append("Files:\n");
		for (final Child c : children)
		{
			builder.append('\t').append(c.name).append('\n');
		}
		
		return builder.toString();
	}

	@Override
	protected void parse(final ByteReader reader) throws IOException
	{
		name = reader.readString();
		currentPath = reader.readString();
		final int numDirs = reader.readInt();
		for (int i = 0; i < numDirs; i++)
		{
			subDirs.add(reader.readString());
		}
		final int numFiles = reader.readInt();
		for (int i = 0; i < numFiles; i++)
		{
			children.add(new Child(reader));
		}
	}

	@Override
	protected void print(Communication connection, final AbstractByteWriter buffer) throws IOException
	{
		buffer.append(name);
		buffer.append(currentPath);
		buffer.append(subDirs.size());
		for (final String sub : subDirs)
		{
			buffer.append(sub);
		}
		buffer.append(children.size());
		for (final Child c : children)
		{
			c.write(buffer);
		}
	}

	@Override
	public void perform(final Communication connection) throws Exception
	{
		getRoot(connection.getMachine());
		final RemoteSynchronizerQueue sync = Services.syncs.getSynchronizer(connection, getRoot());
		if (sync == null)
		{
			LogWrapper.getLogger().info("Lost synchronizer?");
			return;
		}
		sync.receiveList(this);
	}

	public String getCurrentPath()
	{
		return getPath().getFullPath();
	}
	
	RemoteDirectory rootCache;
	RemoteDirectory getRoot(final Machine machine)
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
		
		Child(final SharedFile l)
		{
			this.name = l.getPath().getUnbrokenName();
			this.size = l.getFileSize();
			this.checksum = l.getChecksum() == null ? "" : l.getChecksum();
			this.tags = l.getTags();
			this.lastModified = l.getLastUpdated();
		}
		
		Child (final ByteReader bytes) throws IOException
		{
			name = bytes.readString();
			size = bytes.readLong();
			checksum = bytes.readString();
			tags = bytes.readString();
			lastModified = bytes.readLong();
		}
		
		public void write(final AbstractByteWriter buffer) throws IOException
		{
			buffer.append(name);
			buffer.append(size);
			buffer.append(checksum);
			buffer.append(tags == null ? "" : tags);
			buffer.append(lastModified);
		}
		
		public RemoteFile create() 
		{
			final PathElement pathElement = DbPaths.getPathElement(getPath(), name);
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
