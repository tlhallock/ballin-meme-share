package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;

public class RequestAccess extends Message
{
	public static int TYPE = 7;
	
	private boolean share;
	private boolean message;
	private HashSet<String> roots;
	
	public RequestAccess(Machine m)
	{
		
	}

	public RequestAccess(InputStream i) throws IOException
	{
		super(i);
	}
	
	protected int getType()
	{
		return TYPE;
	}
	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
	}
	@Override
	protected void write(AbstractByteWriter buffer)
	{
		
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Please give me access.");
		
		return builder.toString();
	}
}
