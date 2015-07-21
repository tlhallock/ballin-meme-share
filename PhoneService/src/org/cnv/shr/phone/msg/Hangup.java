package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.Services;

public class Hangup extends PhoneMessage
{
	public int code;
	public String message;
	
	public Hangup(ConnectionParams params)
	{
		super(params);
	}
	public Hangup(int code, String message)
	{
		this.code = code;
		this.message = message;
	}

	@Override
	public void perform(PhoneLine line, MsgHandler listener)
	{
		listener.onError(this);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("code", code);
		generator.write("message", message);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsCode = true;
		boolean needsMessage = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsCode)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs code");
				}
				if (needsMessage)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs message");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("code")) {
					needsCode = false;
					code = Integer.parseInt(parser.getString());
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("message")) {
					needsMessage = false;
					message = parser.getString();
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "Hangup"; }
	public String getJsonKey() { return getJsonName(); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = Services.createGenerator(output, true);)         {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
