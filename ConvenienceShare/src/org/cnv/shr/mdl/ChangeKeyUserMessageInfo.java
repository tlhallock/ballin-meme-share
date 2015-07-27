package org.cnv.shr.mdl;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class ChangeKeyUserMessageInfo implements Jsonable
{
	String url;
	String machineName;
	String machineIdentifier; 
	String key;
	
	public ChangeKeyUserMessageInfo(String url, Machine machine, RSAPublicKey key)
	{
		this.url = url;
		this.machineName = machine.getName();
		this.machineIdentifier = machine.getIdentifier();
		this.key = KeyPairObject.serialize(key);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("url", url);
		generator.write("machineName", machineName);
		generator.write("machineIdentifier", machineIdentifier);
		generator.write("key", key);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsUrl = true;
		boolean needsMachineName = true;
		boolean needsMachineIdentifier = true;
		boolean needsKey = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsUrl)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeKeyUserMessageInfo\" needs \"url\"");
				}
				if (needsMachineName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeKeyUserMessageInfo\" needs \"machineName\"");
				}
				if (needsMachineIdentifier)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeKeyUserMessageInfo\" needs \"machineIdentifier\"");
				}
				if (needsKey)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeKeyUserMessageInfo\" needs \"key\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "url":
					needsUrl = false;
					url = parser.getString();
					break;
				case "machineName":
					needsMachineName = false;
					machineName = parser.getString();
					break;
				case "machineIdentifier":
					needsMachineIdentifier = false;
					machineIdentifier = parser.getString();
					break;
				case "key":
					needsKey = false;
					key = parser.getString();
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ChangeKeyUserMessageInfo"; }
	public String getJsonKey() { return getJsonName(); }
	public ChangeKeyUserMessageInfo(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}