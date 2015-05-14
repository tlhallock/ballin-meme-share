package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;

public class FindMachines extends Message
{
	public static int TYPE = 4;
	
	public FindMachines() {}
	
	public FindMachines(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}
	
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
