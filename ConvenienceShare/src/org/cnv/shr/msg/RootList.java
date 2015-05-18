package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.AbstractByteWriter;
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
	
	public RootList(InputStream i) throws IOException
	{
		super(i);
	}
	
	private void add(RootDirectory root)
	{
		sharedDirectories.add(root);
	}

	@Override
	public void perform(Communication connection)
	{
		HashSet<String> accountedFor = new HashSet<>();
		boolean changed = true;
		for (RootDirectory root : sharedDirectories)
		{
			accountedFor.add(root.getName());
			root.setMachine(connection.getMachine());
			try
			{
				root.save();
			}
			catch (SQLException e)
			{
				Services.logger.print(e);
			}
		}
		
		DbIterator<RootDirectory> list = DbRoots.list(connection.getMachine());
		while (list.hasNext())
		{
			RootDirectory next = list.next();
			if (accountedFor.contains(next.getName()))
			{
				continue;
			}
			Services.logger.println("Should delete root: " + next);
			changed = true;
		}
		
		if (changed)
		{
			Services.notifications.remotesChanged();
		}
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		int numFolders = reader.readInt();
		for (int i = 0; i < numFolders; i++)
		{
			String name        = reader.readString();
			String tags        = reader.readString();
			String description = reader.readString();
			
			sharedDirectories.add(new RemoteDirectory(null, name, tags, description));
		}
	}
	

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
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
	@Override
	protected int getType()
	{
		return TYPE;
	}

	
	@Override
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
