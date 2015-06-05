package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChunkList extends DownloadMessage
{
	private List<Chunk> chunks = new LinkedList<>();

	public static int TYPE = 11;
	
	public ChunkList(InputStream input) throws IOException
	{
		super(input);
	}
	
	public ChunkList(List<Chunk> chunks2, FileEntry descriptor)
	{
		super(descriptor);
		chunks = chunks2;
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
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor(), connection);
		if (downloadInstance == null)
		{
			return;
		}
		downloadInstance.foundChunks(connection.getMachine(), chunks);
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
	

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.writeStartArray("chunks");
		for (org.cnv.shr.dmn.dwn.Chunk elem : chunks)
		{
		elem.generate(generator);
		}
		generator.writeEnd();
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needschunks = true;
		boolean needsdescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needschunks)
				{
					throw new RuntimeException("Message needs chunks");
				}
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_ARRAY:
			if (key==null) break;
			if (key.equals("chunks")) {
				needschunks = false;
				chunks = new LinkedList<>();















				while (parser.hasNext())                    
				{                                           
					e = parser.next();                        
					switch (e)                                
					{                                         
					case START_ARRAY:                         
					case START_OBJECT:                        
					case VALUE_TRUE:                          
					case VALUE_NUMBER:                        
					case VALUE_STRING:                        
						if (key == null)                        
								break;                              
					case END_ARRAY:                           
						break;                                  
					default:                                  
						break;                                  
					}                                         
				}                                           















;
			}
			break;
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("descriptor")) {
				needsdescriptor = false;
				descriptor = new FileEntry(parser);
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "ChunkList"; }
	public ChunkList(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
