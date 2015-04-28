package org.cnv.shr.mdl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import org.cnv.shr.dmn.Notifications;
import org.cnv.shr.util.Find;

public class LocalDirectory
{
	private File path;
	private HashMap<String, LocalFile> files;
	long fileSize;
	
	public LocalDirectory(File localDirectory)
	{
		path = localDirectory;
		files = new HashMap<>();
	}

	public boolean contains(File f)
	{
		return f.getAbsolutePath().startsWith(path.getAbsolutePath());
	}

	public void synchronizeLarge()
	{
		throw new UnsupportedOperationException("Implement me!");
	}

	public void synchronize()
	{
		final HashSet<String> filesToRefresh = new HashSet<>();
		Find.find(path, new Find.FileListener()
		{	
			@Override
			public void fileFound(File f)
			{
				filesToRefresh.add(f.getAbsolutePath());
			}
		});
		filesToRefresh.addAll(files.keySet());
		

		long tmpFileSize = 0;
		HashSet<String> filesToRemove = new HashSet<>();
		for (String path : filesToRefresh)
		{
			LocalFile file = getFile(path);
			if (!file.refresh())
			{
				filesToRemove.add(path);
			}
			else
			{
				tmpFileSize += file.filesize;
			}
		}
		fileSize = tmpFileSize;
		
		for (String path : filesToRemove)
		{
			files.remove(path);
		}

		Notifications.localsChanged();
	}

	public LocalFile getFile(String path)
	{
		LocalFile local = files.get(path);
		if (local == null)
		{
			local = new LocalFile(path);
			files.put(path, local);
		}
		return local;
	}
	
	public String toString()
	{
		return path + " [" + files.size() + " files] [disk usage: " + getFileSize() + "]";
	}
	
	public long getFileSize()
	{
		return fileSize;
	}
}
