package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Connection;
import org.cnv.shr.util.ByteListBuffer;

public class Wait extends Message
{
	public static int TYPE = 8;
	
	public Wait(InetAddress a, InputStream i) throws IOException
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
	public void perform(Connection connection) throws Exception
	{
		// TODO Auto-generated method stub
		
	}
}
