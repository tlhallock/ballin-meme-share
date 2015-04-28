package org.cnv.shr.msg;

import java.util.LinkedList;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;

public class UserList extends Message
{
	LinkedList<Machine> users = new LinkedList<>();

	public UserList()
	{
			for (Machine machine : Services.remotes.getMachines())
			{
				users.add(machine);
			}
	}

	@Override
	public void perform()
	{
		// Remotes.getInstance().addUser(); ...
	}
}
