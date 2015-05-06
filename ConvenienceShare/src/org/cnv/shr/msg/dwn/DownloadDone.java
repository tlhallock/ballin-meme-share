package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class DownloadDone extends Message
{
	public DownloadDone() {}
	
	protected DownloadDone(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}

	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(ByteListBuffer buffer) {}

	public static int TYPE = 15;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Services.server.getServeInstance(connection).quit();
	}
}
