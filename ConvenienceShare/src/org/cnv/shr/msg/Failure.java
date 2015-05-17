package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class Failure extends Message
{
	private String message;
	
	public Failure(String message)
	{
		this.message = message;
	}
	public Failure(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	public void perform(Communication connection)
	{
		System.out.println("Unable to perform request:" + message);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		message = reader.readString();
	}

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
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
