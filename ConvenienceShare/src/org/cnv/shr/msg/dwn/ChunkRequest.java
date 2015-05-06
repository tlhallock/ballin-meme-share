package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class ChunkRequest extends Message
{
	private Chunk chunk;

	public static int TYPE = 16;
	public ChunkRequest(Chunk removeFirst)
	{
		this.chunk = removeFirst;
	}
	
	public ChunkRequest(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		chunk = new Chunk(bytes);
	}
	@Override
	protected void write(ByteListBuffer buffer)
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
}
