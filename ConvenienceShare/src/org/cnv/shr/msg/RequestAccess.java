package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;

public class RequestAccess extends Message
{
	public static int TYPE = 7;
	
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
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void write(ByteListBuffer buffer)
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		// TODO Auto-generated method stub
		
	}
}
