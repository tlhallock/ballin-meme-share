package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

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
		throw new RuntimeException("Please implement me");
//		Machine machine = connection.getMachine();
//		machine.setLastActive(System.currentTimeMillis());
//		try
//		{
//			machine.save();
//		}
//		catch (SQLException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to save machine", e);
//		}
	}

	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) {}
	
	public static int TYPE = 5;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("My heart is beating.");
		
		return builder.toString();
	}

}
