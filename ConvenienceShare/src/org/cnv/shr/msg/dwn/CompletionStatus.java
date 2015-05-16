package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class CompletionStatus extends DownloadMessage
{
	public static int TYPE = 12;
	
	private double percentComplete;
	
	public CompletionStatus(SharedFileId descriptor, double d)
	{
		super(descriptor);
		percentComplete = d;
	}

	public CompletionStatus(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		percentComplete = reader.readDouble();
	}
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
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
