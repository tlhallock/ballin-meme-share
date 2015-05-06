package org.cnv.shr.dmn.dwn;

import java.util.HashMap;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.LocalFile;

public class ServeManager
{
	private HashMap<String, ServeInstance> serves = new HashMap<>();
	
	public ServeInstance getServeInstance(Communication communication)
	{
		return serves.get(communication.getUrl());
	}

	public ServeInstance serve(LocalFile file, Communication c)
	{
		ServeInstance instance = new ServeInstance(c, file);
		serves.put(c.getUrl(), instance);
		return instance;
	}
	
	public void done(Communication c)
	{
		serves.remove(c.getUrl());
	}
}
