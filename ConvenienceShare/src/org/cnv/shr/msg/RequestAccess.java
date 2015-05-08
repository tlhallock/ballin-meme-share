package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashSet;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;

public class RequestAccess extends Message
{
	public static int TYPE = 7;
	
	private boolean share;
	private boolean message;
	private HashSet<String> roots;
	
	public RequestAccess(Machine m)
	{
		
	}

	public RequestAccess(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}
	
	protected int getType()
	{
		return TYPE;
	}
	@Override
	protected void parse(InputStream bytes) throws IOException
	{
	}
	@Override
	protected void write(ByteListBuffer buffer)
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
