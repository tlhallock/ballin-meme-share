package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class FileRequest extends DownloadMessage
{
	private int chunkSize;
	private LinkedList<Chunk> chunks = new LinkedList<>();

	public static int TYPE = 13;

	public FileRequest(RemoteFile remoteFile, int chunkSize)
	{
		super(new SharedFileId(remoteFile));
		this.chunkSize = chunkSize;
	}
	
	public FileRequest(InputStream stream) throws IOException
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
		
		builder.append("I wanna download " + getDescriptor() + " in chunksizes " + chunkSize);
		
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LocalFile local = getDescriptor().getLocal();
		SharingState sharing = DbPermissions.isSharing(connection.getMachine(), local.getRootDirectory());
		if (!checkPermissionsDownloadable(connection.getMachine(), local.getRootDirectory()))
		{
//			fail("Not downloadable");
			connection.finish();
			return;
		}
		ServeInstance serve = Services.server.serve(local, connection, chunkSize);
		serve.sendChunks(chunks);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		chunkSize = reader.readInt();
		
	}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(chunkSize);
	}
}
