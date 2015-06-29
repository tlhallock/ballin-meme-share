package org.cnv.shr.prts;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;

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
		boolean needsprotocol = true;
		boolean needsdescription = true;
		boolean needsinternalPort = true;
		boolean needsexternalPort = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsprotocol)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs protocol");
				}
				if (needsdescription)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs description");
				}
				if (needsinternalPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs internalPort");
				}
				if (needsexternalPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs externalPort");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ip":
				ip = parser.getString();
				break;
			case "protocol":
				needsprotocol = false;
				protocol = parser.getString();
				break;
			case "description":
				needsdescription = false;
				description = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "internalPort":
				needsinternalPort = false;
				internalPort = Integer.parseInt(parser.getString());
				break;
			case "externalPort":
				needsexternalPort = false;
				externalPort = Integer.parseInt(parser.getString());
				break;
			}
			break;
			default: break;
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
