package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class CompletionStatus extends Message
{
	public static int TYPE = 12;
	
	double percentComplete;
	
	public CompletionStatus(double d)
	{
		percentComplete = d;
	}

	public CompletionStatus(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		percentComplete = ByteReader.readDouble(bytes);
	}
	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(percentComplete);
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		Services.server.getServeInstance(connection).setPercentComplete(percentComplete);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("remote is " + percentComplete + " done.");
		
		return builder.toString();
	}
}
