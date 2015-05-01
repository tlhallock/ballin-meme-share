package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Connection;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class ListFiles extends Message
{
	public ListFiles() {}
	public ListFiles(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	public void perform(Connection connection)
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
}
