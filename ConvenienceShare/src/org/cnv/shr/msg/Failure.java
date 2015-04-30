package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Connection;
import org.cnv.shr.util.ByteListBuffer;

public class Failure extends Message
{
	public Failure() {}
	public Failure(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	public void perform(Connection connection)
	{
		System.out.println("Unable to perform request.");
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
	
	public static int TYPE = 2;
	protected int getType()
	{
		return TYPE;
	}
}
