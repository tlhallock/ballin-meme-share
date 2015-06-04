package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChecksumRequest extends Message
{
	public static int TYPE = 32;
	
	private SharedFileId descriptor;

	public ChecksumRequest(RemoteFile remoteFile)
	{
		descriptor = new SharedFileId(remoteFile);
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
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I wanna download " + descriptor);
		
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LocalFile local = descriptor.getLocal();
		checkPermissionsDownloadable(connection, connection.getMachine(), local.getRootDirectory(), "Creating checksum");
		local.ensureChecksummed();
		connection.send(new ChecksumResponse(local));
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		descriptor = reader.readSharedFileId();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(descriptor);
	}
}
