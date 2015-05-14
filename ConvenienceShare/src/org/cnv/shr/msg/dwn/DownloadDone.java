package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;

public class DownloadDone extends Message
{
	public DownloadDone() {}

	public DownloadDone(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	public void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}

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
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Download done.");
		
		return builder.toString();
	}
}
