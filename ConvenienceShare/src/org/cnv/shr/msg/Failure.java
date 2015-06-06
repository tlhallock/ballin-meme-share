package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class Failure extends Message
{
	private String message;
	
	public Failure(String message)
	{
		this.message = message;
	}
	public Failure(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	public void perform(Communication connection)
	{
		System.out.println("Unable to perform request:" + message);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		message = reader.readString();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(message);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Failure message:" + message);
		
		return builder.toString();
	}
	
	public static int TYPE = 2;
	protected int getType()
	{
		return TYPE;
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (message!=null)
		generator.write("message", message);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsmessage = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsmessage)
				{
					throw new RuntimeException("Message needs message");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
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
	public String getJsonName() { return "Failure"; }
	public Failure(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
