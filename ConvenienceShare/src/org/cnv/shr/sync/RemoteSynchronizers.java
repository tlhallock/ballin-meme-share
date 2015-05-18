package org.cnv.shr.sync;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteDirectory;

public class RemoteSynchronizers
{
	public Hashtable<String, RemoteSynchronizerQueue> synchronizers = new Hashtable<>();
	
	private String getKey(RemoteSynchronizerQueue s)
	{
		return getKey(s.communication, s.root);
	}
	private String getKey(Communication c, RemoteDirectory r)
	{
		return c.getUrl() + "::" + r.getName();
	}
	
	public RemoteSynchronizerQueue getSynchronizer(Communication c, RemoteDirectory r)
	{
		return synchronizers.get(getKey(c, r));
	}

	public RemoteSynchronizerQueue createRemoteSynchronizer(RemoteDirectory root) throws UnknownHostException, IOException
	{
		Communication c = Services.networkManager.openConnection(root.getMachine(), false);
		if (c == null)
		{
			throw new IOException("Unable to connect to remote!");
		}
		RemoteSynchronizerQueue returnValue = new RemoteSynchronizerQueue(c, root);
		synchronizers.put(getKey(returnValue), returnValue);
		return returnValue;
	}
	
	public void done(RemoteSynchronizerQueue sync)
	{
		synchronizers.remove(getKey(sync));
	}

	public void closeAll()
	{
		for (RemoteSynchronizerQueue s : synchronizers.values())
		{
			s.close();
		}
	}
}
