package org.cnv.shr.mdl;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.cnv.shr.dmn.Notifications;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Find;

public class LocalDirectory extends RootDirectory
{
	public LocalDirectory(Machine machine, File localDirectory)
	{
		super(machine, localDirectory.getAbsolutePath());
	}
	
	public boolean contains(File f)
	{
		return f.getAbsolutePath().startsWith(path);
	}

	public void synchronize()
	{
		class LocalVars
		{
			boolean changed = false;
			long tmpFileSize = 0;
		}
		final LocalVars vars = new LocalVars();

		// Double check to make sure this directory is here and in the db...
		
		final HashMap<String, LocalFile> currentLocals = Services.db.list(this);

		HashSet<String> filesToRemove = new HashSet<>();
		for (Entry<String, LocalFile> entry : currentLocals.entrySet())
		{
			LocalFile file = entry.getValue();
			String path = entry.getKey();

			if (!file.exists())
			{
				filesToRemove.add(path);
				vars.changed = true;
			}
			else
			{
				vars.tmpFileSize += file.filesize;
			}
			
			vars.changed |= file.refreshAndWriteToDb();
		}
		
		Find.find(new File(path), new Find.FileListener()
		{	
			LinkedList<SharedFile> list = new LinkedList<>();
			
			@Override
			public synchronized void fileFound(File f)
			{
				String absolutePath = f.getAbsolutePath();
				if (currentLocals.get(absolutePath) == null)
				{
					LocalFile newFile = new LocalFile(getThis(), absolutePath);
					list.add(newFile);
					vars.changed = true;
					vars.tmpFileSize += newFile.filesize;
					
					if (list.size() > 50)
					{
						flush();
					}
				}
			}

			@Override
			public synchronized void flush()
			{
				try
				{
					Services.db.addFiles(getThis(), list);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
					return;
				}
				list.clear();
			}
		});
		
		totalFileSize = vars.tmpFileSize;
		
		if (vars.changed)
		{
			Notifications.localsChanged();
		}
	}
	
	private LocalDirectory getThis()
	{
		return this;
	}

	public LocalFile getFile(String path)
	{
		return Services.db.getFile(Services.localMachine, this, new File(path).getName());
	}
	
	public String toString()
	{
		return path /* + " [" + files.size() + " files] [disk usage: " + getFileSize() + "]" */;
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}
}
