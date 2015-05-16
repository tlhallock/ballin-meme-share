package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DownloadFailure extends DownloadMessage
{
	private String message;
	
	public DownloadFailure(String message, SharedFileId descriptor)
	{
		super(descriptor);
		this.message = message;
	}

	public DownloadFailure(InputStream stream) throws IOException
	{
		super(stream);
	}

	public static int TYPE = 21;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection)
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor());
		downloadInstance.removePeer(connection);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		message = reader.readString();
	}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(message);
	}
}
