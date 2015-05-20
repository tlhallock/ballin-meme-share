package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DownloadDone extends DownloadMessage
{
	public DownloadDone(SharedFileId descriptor) { super(descriptor); }

	public DownloadDone(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException {}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) {}

	public static int TYPE = 15;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		ServeInstance serveInstance = Services.server.getServeInstance(connection);
		if (serveInstance == null)
		{
			connection.finish();
		}
		serveInstance.quit();
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Download done.");
		
		return builder.toString();
	}
}
