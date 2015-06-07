package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class DoneResponse extends Message
{
	public DoneResponse() {}
	
	public DoneResponse(InputStream input) throws IOException
	{
		super(input);
	}

	public static int TYPE = 31;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.setDone();
		connection.getSocket().close();
	}

	public boolean requiresAthentication()
	{
		return false;
	}
	
	public String toString()
	{
		return "I am done too.";
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
	public static String getJsonName() { return "DoneResponse"; }
	public DoneResponse(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
