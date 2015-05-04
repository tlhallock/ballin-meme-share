package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class HeartBeat extends Message
{
	public HeartBeat() {}
	
	public HeartBeat(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}
	
	@Override
	public void perform(Communication connection)
	{
		Services.remotes.isAlive(getMachine());
	}

	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(ByteListBuffer buffer) {}
	
	public static int TYPE = 5;
	protected int getType()
	{
		return TYPE;
	}

}
