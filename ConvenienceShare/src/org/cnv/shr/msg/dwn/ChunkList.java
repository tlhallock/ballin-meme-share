package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChunkList extends Message
{
	private LinkedList<Chunk> chunks = new LinkedList<>();
	private String checksum;

	public static int TYPE = 11;
	
	public ChunkList(InputStream input) throws IOException
	{
		super(input);
	}
	
	public ChunkList(HashMap<String, Chunk> chunks2, String checksum)
	{
		for (Chunk c : chunks2.values())
		{
			chunks.add(c);
		}
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}
	@Override
	public void parse(InputStream bytes) throws IOException
	{
		int numChunks = ByteReader.readInt(bytes);
		for (int i = 0; i < numChunks; i++)
		{
			chunks.add(new Chunk(bytes));
		}
		checksum = ByteReader.readString(bytes);
	}
	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(chunks.size());
		for (Chunk c : chunks)
		{
			c.write(buffer);
		}
		buffer.append(checksum);
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(connection);
		downloadInstance.foundChunks(chunks, checksum);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Listing chunks:");
		for (Chunk c : chunks)
		{
			builder.append(c);
		}
		
		return builder.toString();
	}
}
