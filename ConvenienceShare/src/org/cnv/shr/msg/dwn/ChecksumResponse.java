package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChecksumResponse extends Message
{
	public static int TYPE = 33;
	private SharedFileId descriptor;
	private String checksum;

	public ChecksumResponse(SharedFile shared)
	{
		descriptor = new SharedFileId(shared);
		checksum = shared.getChecksum();
	}
	
	public ChecksumResponse(InputStream stream) throws IOException
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
		builder.append("Here is your checksum for ").append(descriptor).append(" ").append(checksum);
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		RemoteFile remoteFile = descriptor.getRemote();
		if (DbDownloads.getPendingDownloadId(remoteFile) == null)
		{
			return;
		}
		
		if (remoteFile.getChecksum() == null)
		{
			remoteFile.setChecksum(checksum);
			remoteFile.tryToSave();
		}
		
		UserActions.download(remoteFile);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		descriptor = reader.readSharedFileId();
		checksum = reader.readString();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(descriptor);
		buffer.append(checksum);
	}
}
