package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;

public class HeartBeat extends Message
{
	public HeartBeat() {
		}
	
	public HeartBeat(InputStream i) throws IOException
	{
		super(i);
	}
	
	@Override
	public void perform(Communication connection)
	{
		Machine machine = connection.getMachine();
		machine.setLastActive(System.currentTimeMillis());
		try
		{
			machine.save();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}
	
	public static int TYPE = 5;
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("My heart is beating.");
		
		return builder.toString();
	}

}
