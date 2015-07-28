package org.cnv.shr.mdl;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class ChangeIdentifierUserMessage implements Jsonable
{
	String newIdentifer;
	String ip;
	int port;
	private String name;
	
	@MyParserNullable
	private String publicKey;
	
	public ChangeIdentifierUserMessage(
			String newIdentifer, 
			String ip, 
			int port, 
			RSAPublicKey publicKey,
			String name)
	{
		this.newIdentifer = newIdentifer;
		this.ip = ip;
		this.port = port;
		if (publicKey != null)
			this.publicKey = KeyPairObject.serialize(publicKey);
		this.name = name;
	}

	public void add()
	{
		DbMachines.updateMachineInfo(newIdentifer, name, KeyPairObject.deSerializePublicKey(publicKey), ip, port);
	}
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("newIdentifer", newIdentifer);
		generator.write("ip", ip);
		generator.write("port", port);
		generator.write("name", name);
		if (publicKey!=null)
		generator.write("publicKey", publicKey);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsPort = true;
		boolean needsNewIdentifer = true;
		boolean needsIp = true;
		boolean needsName = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsPort)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeIdentifierUserMessage\" needs \"port\"");
				}
				if (needsNewIdentifer)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeIdentifierUserMessage\" needs \"newIdentifer\"");
				}
				if (needsIp)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeIdentifierUserMessage\" needs \"ip\"");
				}
				if (needsName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.mdl.ChangeIdentifierUserMessage\" needs \"name\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("port")) {
					needsPort = false;
					port = Integer.parseInt(parser.getString());
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "newIdentifer":
					needsNewIdentifer = false;
					newIdentifer = parser.getString();
					break;
				case "ip":
					needsIp = false;
					ip = parser.getString();
					break;
				case "name":
					needsName = false;
					name = parser.getString();
					break;
				case "publicKey":
					publicKey = parser.getString();
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ChangeIdentifierUserMessage"; }
	public String getJsonKey() { return getJsonName(); }
	public ChangeIdentifierUserMessage(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
