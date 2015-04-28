package org.cnv.shr.mdl;

public class RemoteDirectory
{
	String path;
	RemoteFile[] cache;
	Machine remote;

	void refresh()
	{
		// new ListFiles().send(remote);
	}

	RemoteFile[] list()
	{
		if (cache == null)
		{
			refresh();
		}
		return cache;
	}

	void setFiles(RemoteFile[] files)
	{

	}
}
