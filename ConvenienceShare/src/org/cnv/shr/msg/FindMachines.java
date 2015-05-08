package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Communication;
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
	public void perform(Communication connection) throws Exception
	{
		connection.send(new MachineFound());
		DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();
		while (listRemoteMachines.hasNext())
		{
			MachineFound m = new MachineFound(listRemoteMachines.next());
			if (m.equals(connection.getMachine()))
			{
				continue;
			}
			
			connection.send(m);
		}
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Find machines");
		
		return builder.toString();
	}
}
