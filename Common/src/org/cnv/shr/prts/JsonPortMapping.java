package org.cnv.shr.prts;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class JsonPortMapping implements Jsonable
{
	@MyParserNullable
	public String ip;
	
	public int internalPort;
	public int externalPort;
	public String protocol;
	public String description;
	
	public JsonPortMapping(String ip, int externalPort, int internalPort, String protocol, String description)
	{
		this.ip = ip;
		this.internalPort = internalPort;
		this.externalPort = externalPort;
		this.protocol = protocol;
		this.description = description;
	}
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		if (ip!=null)
		generator.write("ip", ip);
		generator.write("internalPort", internalPort);
		generator.write("externalPort", externalPort);
		generator.write("protocol", protocol);
		generator.write("description", description);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsInternalPort = true;
		boolean needsExternalPort = true;
		boolean needsProtocol = true;
		boolean needsDescription = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsInternalPort)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.prts.JsonPortMapping\" needs \"internalPort\"");
				}
				if (needsExternalPort)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.prts.JsonPortMapping\" needs \"externalPort\"");
				}
				if (needsProtocol)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.prts.JsonPortMapping\" needs \"protocol\"");
				}
				if (needsDescription)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.prts.JsonPortMapping\" needs \"description\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "internalPort":
					needsInternalPort = false;
					internalPort = Integer.parseInt(parser.getString());
					break;
				case "externalPort":
					needsExternalPort = false;
					externalPort = Integer.parseInt(parser.getString());
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "ip":
					ip = parser.getString();
					break;
				case "protocol":
					needsProtocol = false;
					protocol = parser.getString();
					break;
				case "description":
					needsDescription = false;
					description = parser.getString();
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "JsonPortMapping"; }
	public String getJsonKey() { return getJsonName(); }
	public JsonPortMapping(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
