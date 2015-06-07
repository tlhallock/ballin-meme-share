package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DoneMessage extends Message
{
	public static int TYPE = 1;
	public static int DONE_PADDING = 20;
	
	public DoneMessage() {}
	public DoneMessage(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		for (int i = 0; i < DONE_PADDING; i++)
		{
			buffer.append(0);
		}
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.send(new DoneResponse());
		connection.setDone();
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("done.");
		return builder.toString();
	}
	
	public boolean requiresAthentication()
	{
		return false;
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			}                                      
		}                                        
	}                                          
	public static String getJsonName() { return "DoneMessage"; }
	public DoneMessage(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
