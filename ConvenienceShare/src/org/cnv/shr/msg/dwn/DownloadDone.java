package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DownloadDone extends DownloadMessage
{
	public DownloadDone(FileEntry descriptor) { super(descriptor); }

	public DownloadDone(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException {}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) {}

	public static int TYPE = 15;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		ServeInstance serveInstance = Services.server.getServeInstance(connection);
		if (serveInstance == null)
		{
			connection.finish();
		}
		serveInstance.quit();
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Download done.");
		
		return builder.toString();
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (descriptor!=null)
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
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
			if (key.equals("descriptor")) {
				needsdescriptor = false;
				descriptor = new FileEntry(parser);
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "DownloadDone"; }
	public DownloadDone(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
