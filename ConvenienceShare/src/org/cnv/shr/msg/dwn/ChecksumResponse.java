package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChecksumResponse extends DownloadMessage
{
	public static int TYPE = 32;

	public ChecksumResponse(SharedFile shared)
	{
		super(new SharedFileId(shared));
	}
	
	public ChecksumResponse(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException {}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException {}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Here is your checksum: ").append(getDescriptor());
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		RemoteFile remoteFile = getDescriptor().getRemote();
		remoteFile.setChecksum(getDescriptor().getChecksum());
		remoteFile.save();

		if (!DbDownloads.hasPendingDownload(remoteFile))
		{
			return;
		}
		
		UserActions.download(remoteFile);
	}
}
