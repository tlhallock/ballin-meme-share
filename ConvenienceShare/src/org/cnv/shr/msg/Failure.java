package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class Failure extends Message
{
	String message;
	
	public Failure(String message)
	{
		this.message = message;
	}
	public Failure(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	public void perform(Communication connection)
	{
		System.out.println("Unable to perform request:" + message);
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		message = ByteReader.readString(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(message);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Failure message:" + message);
		
		return builder.toString();
	}
	
	public static int TYPE = 2;
	protected int getType()
	{
		return TYPE;
	}
}
