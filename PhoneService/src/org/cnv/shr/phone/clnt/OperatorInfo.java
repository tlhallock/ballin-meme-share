package org.cnv.shr.phone.clnt;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.Services;
import org.cnv.shr.phone.cmn.Storable;


public class OperatorInfo implements Storable
{
	public String ip;
	public int beginPort;
	public int endPort;
	
	public OperatorInfo(String ip, int beginPort, int endPort)
	{
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
	}

	public OperatorInfo(JsonParser parser)
	{
		parse(parser);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("ip", ip);
		generator.write("beginPort", beginPort);
		generator.write("endPort", endPort);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsBeginPort = true;
		boolean needsEndPort = true;
		boolean needsIp = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsBeginPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs beginPort");
				}
				if (needsEndPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs endPort");
				}
				if (needsIp)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ip");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "beginPort":
					needsBeginPort = false;
					beginPort = Integer.parseInt(parser.getString());
					break;
				case "endPort":
					needsEndPort = false;
					endPort = Integer.parseInt(parser.getString());
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("ip")) {
					needsIp = false;
					ip = parser.getString();
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "OperatorInfo"; }
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
