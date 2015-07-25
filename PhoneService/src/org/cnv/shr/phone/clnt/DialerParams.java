package org.cnv.shr.phone.clnt;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.Services;
import org.cnv.shr.phone.cmn.Storable;

public class DialerParams implements Storable
{
	long HEARTBEAT_PERIOD  = 10 * 60 * 1000;
	long HEARTBEAT_TIMEOUT = Long.MAX_VALUE;
	long CONNECTION_INACTIVE_REPEAT_MILLISECONDS = 10 * 60 * 1000;
	long CONNECTION_ACTIVE_REPEAT_MILLISECONDS = 2 * 60 * 1000;
	
	String ident = "Unnamed";
	Path voiceMailDirectory = Paths.get("voicemail.d");
	
	

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("HEARTBEAT_PERIOD", HEARTBEAT_PERIOD);
		generator.write("HEARTBEAT_TIMEOUT", HEARTBEAT_TIMEOUT);
		generator.write("CONNECTION_INACTIVE_REPEAT_MILLISECONDS", CONNECTION_INACTIVE_REPEAT_MILLISECONDS);
		generator.write("CONNECTION_ACTIVE_REPEAT_MILLISECONDS", CONNECTION_ACTIVE_REPEAT_MILLISECONDS);
		generator.write("ident", ident);
		generator.write("voiceMailDirectory", voiceMailDirectory.toString());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsIdent = true;
		boolean needsVoiceMailDirectory = true;
		boolean needsHEARTBEAT_PERIOD = true;
		boolean needsHEARTBEAT_TIMEOUT = true;
		boolean needsCONNECTION_INACTIVE_REPEAT_MILLISECONDS = true;
		boolean needsCONNECTION_ACTIVE_REPEAT_MILLISECONDS = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.clnt.DialerParams\" needs \"ident\"");
				}
				if (needsVoiceMailDirectory)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.clnt.DialerParams\" needs \"voiceMailDirectory\"");
				}
				if (needsHEARTBEAT_PERIOD)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.clnt.DialerParams\" needs \"HEARTBEAT_PERIOD\"");
				}
				if (needsHEARTBEAT_TIMEOUT)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.clnt.DialerParams\" needs \"HEARTBEAT_TIMEOUT\"");
				}
				if (needsCONNECTION_INACTIVE_REPEAT_MILLISECONDS)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.clnt.DialerParams\" needs \"CONNECTION_INACTIVE_REPEAT_MILLISECONDS\"");
				}
				if (needsCONNECTION_ACTIVE_REPEAT_MILLISECONDS)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.clnt.DialerParams\" needs \"CONNECTION_ACTIVE_REPEAT_MILLISECONDS\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "ident":
					needsIdent = false;
					ident = parser.getString();
					break;
				case "voiceMailDirectory":
					needsVoiceMailDirectory = false;
					voiceMailDirectory = Paths.get(parser.getString());
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "HEARTBEAT_PERIOD":
					needsHEARTBEAT_PERIOD = false;
					HEARTBEAT_PERIOD = Long.parseLong(parser.getString());
					break;
				case "HEARTBEAT_TIMEOUT":
					needsHEARTBEAT_TIMEOUT = false;
					HEARTBEAT_TIMEOUT = Long.parseLong(parser.getString());
					break;
				case "CONNECTION_INACTIVE_REPEAT_MILLISECONDS":
					needsCONNECTION_INACTIVE_REPEAT_MILLISECONDS = false;
					CONNECTION_INACTIVE_REPEAT_MILLISECONDS = Long.parseLong(parser.getString());
					break;
				case "CONNECTION_ACTIVE_REPEAT_MILLISECONDS":
					needsCONNECTION_ACTIVE_REPEAT_MILLISECONDS = false;
					CONNECTION_ACTIVE_REPEAT_MILLISECONDS = Long.parseLong(parser.getString());
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "DialerParams"; }
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
