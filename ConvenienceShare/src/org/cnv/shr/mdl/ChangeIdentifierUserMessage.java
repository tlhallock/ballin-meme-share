package org.cnv.shr.mdl;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.KeyPairObject;

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
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
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
