package org.cnv.shr.msg;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.cnv.shr.dmn.Remotes;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.User;

public class ListUsers extends Message
{
	HashMap<Machine, User> users = new HashMap<>();

	ListUsers()
	{
	}

	@Override
	public void perform() throws UnknownHostException, IOException
	{
		new UserList().send(getMachine());
	}
}
