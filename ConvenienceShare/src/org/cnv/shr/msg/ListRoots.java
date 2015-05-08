package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class ListRoots extends Message
{
	public ListRoots() {}
	public ListRoots(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	public void perform(Communication connection)
	{
		Services.locals.share(connection);
	}

	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(ByteListBuffer buffer) {}
	
	public static int TYPE = 6;
	protected int getType()
	{
		return TYPE;
	}

	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("List your root directories.");
		
		return builder.toString();
	}
}
