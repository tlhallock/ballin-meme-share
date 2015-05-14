package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;

public class RequestCompletionStatus extends Message
{
	public RequestCompletionStatus() {}
	
	public RequestCompletionStatus(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Services.downloads.getDownloadInstance(connection).sendCompletionStatus();
	}

	public static int TYPE = 20;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	

	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Are you done yet?");
		
		return builder.toString();
	}
}
