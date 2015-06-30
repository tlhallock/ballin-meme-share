package org.cnv.shr.updt;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;

public class LastVersionEntry implements Jsonable
{
	private String identifier;
	private String version;

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("identifier", identifier);
		generator.write("version", version);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsidentifier = true;
		boolean needsversion = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsidentifier)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs identifier");
				}
				if (needsversion)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs version");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "identifier":
				needsidentifier = false;
				identifier = parser.getString();
				break;
			case "version":
				needsversion = false;
				version = parser.getString();
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "LastVersionEntry"; }
	public String getJsonKey() { return getJsonName(); }
	public LastVersionEntry(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
