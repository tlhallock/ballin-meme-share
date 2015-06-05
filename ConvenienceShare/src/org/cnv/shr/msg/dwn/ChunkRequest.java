package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChunkRequest extends DownloadMessage
{
	private Chunk chunk;

	public static int TYPE = 16;
	public ChunkRequest(FileEntry descriptor, Chunk removeFirst)
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
		checkPermissionsDownloadable(connection, connection.getMachine(), getLocal().getRootDirectory(), "Sending chunk.");
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

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		chunk.generate(generator);
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needschunk = true;
		boolean needsdescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needschunk)
				{
					throw new RuntimeException("Message needs chunk");
				}
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_OBJECT:
			if (key==null) break;
			switch(key) {
			case "chunk":
				needschunk = false;
				chunk = JsonThing.readChunk(parser);
				break;
			case "descriptor":
				needsdescriptor = false;
				descriptor = new FileEntry(parser);
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "ChunkRequest"; }
	public ChunkRequest(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
