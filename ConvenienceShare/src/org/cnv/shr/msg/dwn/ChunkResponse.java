package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChunkResponse extends DownloadMessage
{
	private Chunk chunk;
	
	public static int TYPE = 14;
	
	public ChunkResponse(SharedFileId descriptor, Chunk c)
	{
		super(descriptor);
		chunk = c;
	}
	
	public ChunkResponse(InputStream stream) throws IOException
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
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor());
		downloadInstance.download(chunk, connection);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Giving you chunk " + chunk);
		
		return builder.toString();
	}
}
