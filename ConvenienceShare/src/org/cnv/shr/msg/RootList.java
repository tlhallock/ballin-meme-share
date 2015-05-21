package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
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
		Machine machine = connection.getMachine();
		
		boolean changed = true;
		for (RootDirectory root : sharedDirectories)
		{
			accountedFor.add(root.getName());
			root.setMachine(machine);
			try
			{
				root.save();
				changed = true;
			}
			catch (SQLException e)
			{
				Services.logger.print(e);
			}
		}
		
		List<RootDirectory> toDelete = new LinkedList<>();
		DbIterator<RootDirectory> list = DbRoots.list(machine);
		while (list.hasNext())
		{
			final RootDirectory next = list.next();
			if (accountedFor.contains(next.getName()))
			{
				continue;
			}
			toDelete.add(next);
			DbRoots.deleteRoot(next);
			changed = true;
		}
		
		for (RootDirectory root : toDelete)
		{
			DbRoots.deleteRoot(root);
			// should be deleted...
			Services.notifications.remoteDirectoryChanged((RemoteDirectory) root);
		}
		
		if (changed)
		{
			Services.notifications.remoteChanged(machine);
		}
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		Machine machine = DbMachines.getMachine(reader.readString());
		int numFolders = reader.readInt();
		for (int i = 0; i < numFolders; i++)
		{
			String name        = reader.readString();
			String tags        = reader.readString();
			String description = reader.readString();
			
			sharedDirectories.add(new RemoteDirectory(machine, name, tags, description));
		}
	}

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(Services.localMachine.getIdentifier());
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
