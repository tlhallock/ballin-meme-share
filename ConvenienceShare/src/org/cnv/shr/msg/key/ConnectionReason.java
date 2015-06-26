package org.cnv.shr.msg.key;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ConnectionReason extends KeyMessage
{
	private String reason;
	
	public ConnectionReason(String reason)
	{
		this.reason = reason;
	}
	
	public void perform(Communication communication)
	{
		communication.setReason(reason);
	}

	// Deprecated methods
	@Override
	protected int getType() { return 0; }
	@Override
	protected void parse(ByteReader reader) throws IOException {}
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
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
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs reason");
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
	public static String getJsonName() { return "ConnectionReason"; }
	public String getJsonKey() { return getJsonName(); }
	public ConnectionReason(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
