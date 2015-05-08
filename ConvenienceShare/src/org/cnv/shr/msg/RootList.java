package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class RootList extends Message
{
	private List<RootDirectory> sharedDirectories = new LinkedList<>();

	public RootList()
	{
		DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
		while (listLocals.hasNext())
		{
			add(listLocals.next());
		}
	}
	
	public RootList(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}
	
	private void add(RootDirectory root)
	{
		sharedDirectories.add(root);
	}

	@Override
	public void perform(Communication connection)
	{
		boolean changed = false;
		for (RootDirectory root : sharedDirectories)
		{
			try
			{
				root.save();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		
		if (changed)
		{
			Services.notifications.remotesChanged();
		}
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		Machine machine = getMachine();
		int numFolders = ByteReader.readInt(bytes);
		for (int i = 0; i < numFolders; i++)
		{
			String name        = ByteReader.readString(bytes);
			String tags        = ByteReader.readString(bytes);
			String description = ByteReader.readString(bytes);
			
			sharedDirectories.add(new RemoteDirectory(machine, name, tags, description));
		}
	}
	

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(sharedDirectories.size());
		for (RootDirectory dir : sharedDirectories)
		{
			buffer.append(dir.getName());
			buffer.append(dir.getTags());
			buffer.append(dir.getDescription());
		}
	}
	
	public static int TYPE = 3;
	protected int getType()
	{
		return TYPE;
	}

	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Root directories are: ");
		for (RootDirectory directory : sharedDirectories)
		{
			builder.append(directory.getName()).append(':');
		}
		
		return builder.toString();
	}
}
