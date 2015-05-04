package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.ByteListBuffer;

public class FileRequest extends Message
{
	String directory;

	public FileRequest(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
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
	
	public static int TYPE = 10;
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		// TODO Auto-generated method stub
		
	}
}
