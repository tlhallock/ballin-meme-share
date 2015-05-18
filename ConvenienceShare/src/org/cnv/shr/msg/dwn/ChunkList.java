package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChunkList extends DownloadMessage
{
	private LinkedList<Chunk> chunks = new LinkedList<>();

	public static int TYPE = 11;
	
	public ChunkList(InputStream input) throws IOException
	{
		super(input);
	}
	
	public ChunkList(HashMap<String, Chunk> chunks2, String checksum, SharedFileId descriptor)
	{
		super(descriptor);
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
	protected void finishParsing(ByteReader reader) throws IOException
	{
		int numChunks = reader.readInt();
		for (int i = 0; i < numChunks; i++)
		{
			chunks.add(new Chunk(reader));
		}
	}
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
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
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor());
		downloadInstance.foundChunks(chunks);
	}
	
	@Override
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
