package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChunkRequest extends DownloadMessage
{
	private Chunk chunk;

	public static int TYPE = 16;
	public ChunkRequest(SharedFileId descriptor, Chunk removeFirst)
	{
		super(descriptor);
		this.chunk = removeFirst;
	}
	
	public ChunkRequest(InputStream stream) throws IOException
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
		chunk = new Chunk(reader);
	}
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		chunk.write(buffer);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		checkPermissionsDownloadable(connection, connection.getMachine(), getDescriptor().getLocal().getRootDirectory(), "Sending chunk.");
		Services.server.getServeInstance(connection).serve(chunk);
	}

	public Chunk getChunk()
	{
		return chunk;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Give me chunk " + chunk);
		
		return builder.toString();
	}
}
