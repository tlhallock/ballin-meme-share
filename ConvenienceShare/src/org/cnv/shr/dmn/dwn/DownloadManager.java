package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;

public class DownloadManager
{
	// I need to fix this model, because it doesn't allow multiple downloads from the same peer.
	private HashMap<String, DownloadInstance> downloads = new HashMap<>();

	public void addConnection(DownloadInstance downloadInstance, Communication connection)
	{
		downloads.put(connection.getUrl(), downloadInstance);
	}

	public void download(SharedFile remoteFile) throws UnknownHostException, IOException
	{
		if (remoteFile.isLocal())
		{
			return;
		}
		download((RemoteFile) remoteFile);
	}
	
	public DownloadInstance download(RemoteFile file) throws UnknownHostException, IOException
	{
		DownloadInstance instance = new DownloadInstance(file);
		Communication c = instance.begin();
		downloads.put(c.getUrl(), instance);
		return instance;
	}
	
	public void done(DownloadInstance d)
	{
		for (Seeder seeder : d.getSeeders())
		{
			remove(seeder);
		}
	}
	
	public void remove(Seeder peer)
	{
		peer.done();
		downloads.remove(peer.getConnection().getUrl());
	}
	
	public DownloadInstance getDownloadInstance(Communication communication)
	{
		return downloads.get(communication.getUrl());
	}
}
