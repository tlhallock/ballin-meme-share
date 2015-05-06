package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class ChunkList extends Message
{
	private LinkedList<Chunk> chunks = new LinkedList<>();

	public static int TYPE = 11;
	public ChunkList(HashMap<String, Chunk> chunks2)
	{
		for (Chunk c : chunks2.values())
		{
			chunks.add(c);
		}
	}
	
	public ChunkList(InetAddress address, InputStream stream) throws IOException
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
		int numChunks = ByteReader.readInt(bytes);
		for (int i = 0; i < numChunks; i++)
		{
			chunks.add(new Chunk(bytes));
		}
	}
	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(chunks.size());
		for (Chunk c : chunks)
		{
			c.write(buffer);
		}
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(connection);
		downloadInstance.foundChunks(chunks);
	}
}
