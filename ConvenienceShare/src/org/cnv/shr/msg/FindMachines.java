package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Connection;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;

public class FindMachines extends Message
{
	public static int TYPE = 4;
	
	public FindMachines() {}
	
	public FindMachines(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}
	
	
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(ByteListBuffer buffer) {}
	
	@Override
	public void perform(Connection connection) throws Exception
	{
		connection.send(new MachineFound());
		for (Machine machine : Services.remotes.getMachines())
		{
			connection.send(new MachineFound(machine));
		}
	}
}
