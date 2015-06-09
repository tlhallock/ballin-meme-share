package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class CompletionStatus extends DownloadMessage
{
	public static int TYPE = 12;
	
	private double percentComplete;
	
	public CompletionStatus(FileEntry descriptor, double d)
	{
		super(descriptor);
		percentComplete = d;
	}

	public CompletionStatus(InputStream stream) throws IOException
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
		percentComplete = reader.readDouble();
	}
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(percentComplete);
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		ServeInstance serveInstance = Services.server.getServeInstance(connection);
		if (serveInstance == null)
		{
			connection.finish();
			return;
		}
		serveInstance.setPercentComplete(percentComplete);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("remote is " + percentComplete + " done.");
		
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("percentComplete", percentComplete);
		if (descriptor!=null)
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdescriptor = true;
		boolean needspercentComplete = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
				}
				if (needspercentComplete)
				{
					throw new RuntimeException("Message needs percentComplete");
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
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("percentComplete")) {
				needspercentComplete = false;
				percentComplete = Double.parseDouble(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "CompletionStatus"; }
	public CompletionStatus(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
