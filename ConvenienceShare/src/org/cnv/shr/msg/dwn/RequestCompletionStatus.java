package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class RequestCompletionStatus extends DownloadMessage
{
	public RequestCompletionStatus(FileEntry descriptor) { super (descriptor); }
	
	public RequestCompletionStatus(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		for (DownloadInstance instance : Services.downloads.getDownloadInstances(connection))
		{
			instance.sendCompletionStatus();
		}
	}

	public static int TYPE = 20;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Are you done yet?");
		return builder.toString();
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException {}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException {}
}
