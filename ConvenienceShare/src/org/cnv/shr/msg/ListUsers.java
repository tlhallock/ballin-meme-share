package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.cnv.shr.dmn.Remotes;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.User;
import org.cnv.shr.util.ByteListBuffer;

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

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		// TODO Auto-generated method stub
		
	}
}
