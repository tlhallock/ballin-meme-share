package org.cnv.shr.dmn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.User;

public class Remotes
{
	HashMap<Machine, List<RemoteDirectory>> sharedDirectories = new HashMap<>();

	public void refresh(String ip)
	{

	}

	public User discover(String ip, int port)
	{
		return null;

	}

	public void isAlive(Machine machine)
	{

	}

	public Collection<Machine> getMachines()
	{
		return sharedDirectories.keySet();
	}
}
