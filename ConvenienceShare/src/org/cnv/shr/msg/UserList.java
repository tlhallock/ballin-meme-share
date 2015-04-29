package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;

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
	
	public static int TYPE = 1;
	protected int getType()
	{
		return TYPE;
	}
}
