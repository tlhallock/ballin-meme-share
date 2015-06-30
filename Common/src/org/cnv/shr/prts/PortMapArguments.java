
package org.cnv.shr.prts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class PortMapArguments implements Jsonable
{
	@MyParserNullable
	public String upnpLib;
	@MyParserNullable
	public Integer routerIndex;
	
	public String action;
	
	public JsonList<JsonPortMapping> ports = new JsonList<>(
			new JsonList.Allocator<JsonPortMapping>()
			{
				public JsonPortMapping create(JsonParser parser)
				{
					return new JsonPortMapping(parser);
				}});

	public PortMapArguments(PortMapperAction action)
	{
		this.action = action.name();
	}
	public PortMapArguments(PortMapperAction action, String ipGuess, HashMap<Integer, Integer> desiredPorts)
	{
		this.action = action.name();

		for (Entry<Integer, Integer> entry : desiredPorts.entrySet())
		{
			ports.add(new JsonPortMapping(ipGuess, entry.getKey(), entry.getValue(), "TCP",
					"ConvenienceShare mapping " + entry.getKey() + "->" + entry.getValue()));
		}
	}
	
	public void saveTo(Path path) throws IOException
	{
		LogWrapper.getLogger().info("Saving mapping arguments to " + path);
		try (OutputStream output = Files.newOutputStream(path);
				JsonGenerator generator = TrackObjectUtils.createGenerator(output, true))
		{
			generate(generator, null);
		}
	}


	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		if (upnpLib!=null)
		generator.write("upnpLib", upnpLib);
		if (routerIndex!=null)
		generator.write("routerIndex", routerIndex);
		generator.write("action", action);
		{
			generator.writeStartArray("ports");
			ports.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsports = true;
		boolean needsaction = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsports)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ports");
				}
				if (needsaction)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs action");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("routerIndex")) {
				routerIndex = Integer.parseInt(parser.getString());
			}
			break;
		case START_ARRAY:
			if (key==null) break;
			if (key.equals("ports")) {
				needsports = false;
				ports.parse(parser);
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "upnpLib":
				upnpLib = parser.getString();
				break;
			case "action":
				needsaction = false;
				action = parser.getString();
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "PortMapArguments"; }
	public String getJsonKey() { return getJsonName(); }
	public PortMapArguments(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK

}
