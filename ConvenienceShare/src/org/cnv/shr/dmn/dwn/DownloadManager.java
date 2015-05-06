package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.RemoteFile;

public class DownloadManager
{
	private HashMap<String, DownloadInstance> downloads = new HashMap<>();

	public DownloadInstance download(RemoteFile file) throws UnknownHostException, IOException
	{
		DownloadInstance instance = new DownloadInstance(file);
		Communication c = instance.begin();
		downloads.put(c.getUrl(), instance);
		return instance;
	}
	
	public void done(Communication c)
	{
		downloads.remove(c.getUrl());
	}
	
	public DownloadInstance getDownloadInstance(Communication communication)
	{
		return downloads.get(communication.getUrl());
	}
}
