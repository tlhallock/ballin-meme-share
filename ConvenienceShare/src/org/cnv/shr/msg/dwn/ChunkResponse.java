package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class ChunkResponse extends Message
{
	private Chunk chunk;
	
	public static int TYPE = 11;
	
	
	public ChunkResponse(Chunk c)
	{
		chunk = c;
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
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(connection);
		downloadInstance.download(chunk, connection);
	}
}
