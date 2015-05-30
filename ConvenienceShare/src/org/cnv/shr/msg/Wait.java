package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class Wait extends Message
{
	public static int TYPE = 8;
	
	public Wait(InputStream i) throws IOException
	{
		super(i);
	}
	
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		
	}
	
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer)
	{
		
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Just a sec...");
		
		return builder.toString();
	}
}
