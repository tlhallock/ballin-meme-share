package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.Services;

public class PhoneRing extends PhoneMessage
{
	private String ident;
	private String uniqueKey;
	private int replyPort;
	
	public PhoneRing(ConnectionParams params)
	{
		super(params);
	}
	
	public PhoneRing(String key, int port, String ident, ConnectionParams params)
	{
		super(params);
		this.uniqueKey = key;
		this.ident = ident;
		this.replyPort = port;
	}
	
	@Override
	public void perform(PhoneLine line, MsgHandler listener)
	{
		
	}

	public int getReplyPort()
	{
		return replyPort;
	}

	public String getKey()
	{
		return uniqueKey;
	}
	

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("ident", ident);
		generator.write("uniqueKey", uniqueKey);
		generator.write("replyPort", replyPort);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsReplyPort = true;
		boolean needsIdent = true;
		boolean needsUniqueKey = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsReplyPort)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.msg.PhoneRing\" needs \"replyPort\"");
				}
				if (needsIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.msg.PhoneRing\" needs \"ident\"");
				}
				if (needsUniqueKey)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.msg.PhoneRing\" needs \"uniqueKey\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("replyPort")) {
					needsReplyPort = false;
					replyPort = Integer.parseInt(parser.getString());
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "ident":
					needsIdent = false;
					ident = parser.getString();
					break;
				case "uniqueKey":
					needsUniqueKey = false;
					uniqueKey = parser.getString();
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "PhoneRing"; }
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
