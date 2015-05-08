package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.ByteListBuffer;

public class UpdateCode extends Message
{
	public static int TYPE = 9;
	
	private String url;

	public UpdateCode(InetAddress a, InputStream i) throws IOException
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
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Can I update your code.");
		
		return builder.toString();
	}
}
