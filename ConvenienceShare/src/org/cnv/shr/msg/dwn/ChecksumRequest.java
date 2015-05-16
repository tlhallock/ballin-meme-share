package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChecksumRequest extends DownloadMessage
{
	public static int TYPE = 31;

	public ChecksumRequest(RemoteFile remoteFile)
	{
		super(new SharedFileId(remoteFile));
	}
	
	public ChecksumRequest(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I wanna download " + getDescriptor());
		
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LocalFile local = getDescriptor().getLocal();
		local.ensureChecksummed();
		connection.send(new ChecksumResponse(local));
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException {}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException {}
}
