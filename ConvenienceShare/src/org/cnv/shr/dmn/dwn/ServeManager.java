package org.cnv.shr.dmn.dwn;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalFile;

public class ServeManager
{
	private HashMap<String, ServeInstance> serves = new HashMap<>();
	
	public synchronized ServeInstance getServeInstance(Communication communication)
	{
		return serves.get(communication.getUrl());
	}

	public synchronized ServeInstance serve(LocalFile file, Communication c, int chunkSize)
	{
		for (ServeInstance instance : serves.values())
		{
			if (instance.isServing(c, file))
			{
				return instance;
			}
		}
		ServeInstance instance = new ServeInstance(c, file, chunkSize);
		serves.put(c.getUrl(), instance);
		Services.notifications.serveAdded(instance);
		return instance;
	}
	
	public synchronized void done(Communication c)
	{
		ServeInstance serveInstance = serves.get(c.getUrl());
		Services.notifications.serveRemoved(serveInstance);
		serves.remove(c.getUrl());
	}

	public synchronized List<ServeInstance> getServeInstances()
	{
		LinkedList<ServeInstance> returnValue = new LinkedList<>();
		returnValue.addAll(serves.values());
		return returnValue;
	}
}
