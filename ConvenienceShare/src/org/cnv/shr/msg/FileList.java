package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class FileList extends Message
{
	private List<FilesList> sharedDirectories = new LinkedList<>();

	public FileList() {}
	
	public FileList(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}
	
	private FilesList getList(RootDirectory dir)
	{
		for (FilesList l : sharedDirectories)
		{
			if (l.is(dir))
			{
				return l;
			}
		}
		FilesList l = new FilesList(dir);
		sharedDirectories.add(l);
		return l;
	}
	
	public void add(RootDirectory root, SharedFile sharedFile)
	{
		getList(root).sharedFiles.add(sharedFile);
	}

	@Override
	public void perform(Communication connection)
	{
		boolean changed = false;
		Machine machine = getMachine();
//		for (FilesList l : sharedDirectories)
//		{
//			RootDirectory root = l.root;
//			if (Services.db.getRoot(machine, root.getCanonicalPath()) == null)
//			{
//				Services.db.addRoot(machine, root);
//				changed = true;
//			}
//			for (SharedFile r : l.sharedFiles)
//			{
//				SharedFile file = Services.db.getFile(root, r.getRelativePath(), r.getName()); 
//				if (file == null)
//				{
//					Services.db.addFile(root, r);
//					changed = true;
//				}
//				else
//				{
//					Services.db.updateFile(r);
//				}
//			}
//		}

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
			String path        = ByteReader.readString(bytes);
			String tags        = ByteReader.readString(bytes);
			String description = ByteReader.readString(bytes);

			FilesList list = getList(new RemoteDirectory(machine, path, tags, description));
			int nFiles         = ByteReader.readInt(bytes);
			for (int j = 0; j < nFiles; j++)
			{
				list.sharedFiles.add(new RemoteFile(machine, (RemoteDirectory) list.root, bytes));
			}
		}
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(sharedDirectories.size());
		for (FilesList dir : sharedDirectories)
		{
			buffer.append(dir.root.getCanonicalPath());
			buffer.append(dir.root.getTags());
			buffer.append(dir.root.getDescription());

			buffer.append(dir.sharedFiles.size());
			for (SharedFile file : dir.sharedFiles)
			{
				file.write(buffer);
			}
		}
	}
	
	private static class FilesList
	{
		private RootDirectory root;
		private List<SharedFile> sharedFiles = new LinkedList<>();
		
		FilesList(RootDirectory root)
		{
			this.root = root;
		}
		
		boolean is(RootDirectory root)
		{
			return this.root.getCanonicalPath().equals(root.getCanonicalPath());
		}
	}

	
	public static int TYPE = 3;
	protected int getType()
	{
		return TYPE;
	}
}
