package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class UpdateCode extends Message
{
	public static int TYPE = 9;
	
	private String url;

	public UpdateCode(InputStream i) throws IOException
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
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void print(AbstractByteWriter buffer)
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
