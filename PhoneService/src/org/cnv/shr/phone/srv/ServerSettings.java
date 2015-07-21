package org.cnv.shr.phone.srv;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.Services;
import org.cnv.shr.phone.cmn.Storable;


public class ServerSettings implements Storable
{
	public int metaPortBegin = 7020;
	public int metaPortEnd = 7030;
	
	public int connectionPortBegin = 8020;
	public int connectionPortEnd = 8030;
	
	public Path voiceRootMailPath = Paths.get("voicemail.d");
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("metaPortBegin", metaPortBegin);
		generator.write("metaPortEnd", metaPortEnd);
		generator.write("connectionPortBegin", connectionPortBegin);
		generator.write("connectionPortEnd", connectionPortEnd);
		generator.write("voiceRootMailPath", voiceRootMailPath.toString());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsMetaPortBegin = true;
		boolean needsMetaPortEnd = true;
		boolean needsConnectionPortBegin = true;
		boolean needsConnectionPortEnd = true;
		boolean needsVoiceRootMailPath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsMetaPortBegin)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs metaPortBegin");
				}
				if (needsMetaPortEnd)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs metaPortEnd");
				}
				if (needsConnectionPortBegin)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs connectionPortBegin");
				}
				if (needsConnectionPortEnd)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs connectionPortEnd");
				}
				if (needsVoiceRootMailPath)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs voiceRootMailPath");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "metaPortBegin":
					needsMetaPortBegin = false;
					metaPortBegin = Integer.parseInt(parser.getString());
					break;
				case "metaPortEnd":
					needsMetaPortEnd = false;
					metaPortEnd = Integer.parseInt(parser.getString());
					break;
				case "connectionPortBegin":
					needsConnectionPortBegin = false;
					connectionPortBegin = Integer.parseInt(parser.getString());
					break;
				case "connectionPortEnd":
					needsConnectionPortEnd = false;
					connectionPortEnd = Integer.parseInt(parser.getString());
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("voiceRootMailPath")) {
					needsVoiceRootMailPath = false;
					voiceRootMailPath = Paths.get(parser.getString());
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ServerSettings"; }
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
