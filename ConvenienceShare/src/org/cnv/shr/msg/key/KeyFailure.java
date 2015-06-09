package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class KeyFailure extends KeyMessage
{
	private String reason;
	
	public KeyFailure(String reason)
	{
		this.reason = reason;
	}
	public KeyFailure(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) {}

	public static int TYPE = 26;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	public String toString()
	{
		return "Unable to authenticate: " + reason;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LogWrapper.getLogger().info("Key failure");
		connection.setAuthenticated(false);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (reason!=null)
		generator.write("reason", reason);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsreason = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsreason)
				{
					throw new RuntimeException("Message needs reason");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("reason")) {
				needsreason = false;
				reason = parser.getString();
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "KeyFailure"; }
	public KeyFailure(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
