package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;

public class ChunkRequest extends Message
{
	private Chunk chunk;

	public static int TYPE = 16;
	public ChunkRequest(Chunk removeFirst)
	{
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
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
		chunk = new Chunk(bytes);
	}
	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		chunk.write(buffer);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		Services.server.getServeInstance(connection).serve(chunk);
	}

	public Chunk getChunk()
	{
		return chunk;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Give me chunk " + chunk);
		
		return builder.toString();
	}
}
