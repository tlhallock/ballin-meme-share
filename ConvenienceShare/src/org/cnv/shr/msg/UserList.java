package org.cnv.shr.msg;

import java.util.HashMap;

import org.cnv.shr.dmn.Remotes;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.User;

public class UserList extends Message
{
	HashMap<Machine, User> users = new HashMap<>();

	public UserList()
	{
		for (User user : Remotes.getInstance().getUsers())
		{
			for (Machine machine : user.getMachines())
			{
				users.put(machine, user);
			}
		}
	}

	@Override
	public void perform()
	{
		// Remotes.getInstance().addUser(); ...
	}
}
