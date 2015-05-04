package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.ByteListBuffer;

public class DoneMessage extends Message
{
	public static int TYPE = 1;
	
	public DoneMessage() {}
	public DoneMessage(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}
	
	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(ByteListBuffer buffer) {}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.remoteIsDone();
	}
}
