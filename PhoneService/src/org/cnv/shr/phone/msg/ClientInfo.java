package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.Services;

public class ClientInfo extends PhoneMessage
{
	@MyParserNullable
	private String ip;
	@MyParserNullable
	private int port;
	private String ident;
	private long refreshRate;
	
	public ClientInfo(ConnectionParams params)
	{
		super(params);
	}
	
	public ClientInfo(String ident)
	{
		this.ident = ident;
	}

	public int hashCode()
	{
		return getKeyString().hashCode(); 
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof ClientInfo)
		{
			ClientInfo oinfo = (ClientInfo) other;
			return ip.equals(oinfo.ip) && port == oinfo.port && ident.equals(oinfo.ident);
		}
		return false;
	}

	public void setPort(int port2)
	{
		this.port = port2;
	}

	public void setIp(String hostAddress)
	{
		this.ip = hostAddress;
	}

	private String getKeyString()
	{
		return ip + ":" + port + "[" + ident + "]";
	}

	@Override
	public void perform(PhoneLine line, MsgHandler listener) throws Exception
	{
		listener.onClientInfo(line, this);
	}

	public String getIdent()
	{
		return ident;
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
		generator.write("port", port);
		generator.write("ident", ident);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsIdent = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsIdent)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ident");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("port")) {
					port = Integer.parseInt(parser.getString());
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "ip":
					ip = parser.getString();
					break;
				case "ident":
					needsIdent = false;
					ident = parser.getString();
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ClientInfo"; }
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
