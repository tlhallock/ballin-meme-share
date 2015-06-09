package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DownloadFailure extends DownloadMessage
{
	private String message;
	
	public DownloadFailure(String message, FileEntry descriptor)
	{
		super(descriptor);
		this.message = message;
	}

	public DownloadFailure(InputStream stream) throws IOException
	{
		super(stream);
	}

	public static int TYPE = 41;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection)
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor(), connection);
		downloadInstance.removePeer(connection);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		message = reader.readString();
	}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(message);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (message!=null)
		generator.write("message", message);
		if (descriptor!=null)
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdescriptor = true;
		boolean needsmessage = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
				}
				if (needsmessage)
				{
					throw new RuntimeException("Message needs message");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("descriptor")) {
				needsdescriptor = false;
				descriptor = new FileEntry(parser);
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("message")) {
				needsmessage = false;
				message = parser.getString();
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "DownloadFailure"; }
	public DownloadFailure(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
