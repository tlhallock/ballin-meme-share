package org.cnv.shr.trck;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.util.KeyPairObject;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class MachineEntry implements TrackObject
{
	private String ident;
	private String keyStr;
	private String name;
	
	private String ip;
	private int beginPort;
	private int endPort;
	
	public MachineEntry() {}

	public MachineEntry(String ident, RSAPublicKey key, String ip, int beginPort, int endPort, String name)
	{
		this(ident, KeyPairObject.serialize(key), ip, beginPort, endPort, name);
	}
	
	public MachineEntry(String ident, String key, String ip, int beginPort, int endPort, String name)
	{
		this.ident = ident;
		this.keyStr = key;
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
		this.name = name;
	}

	@Override
	public void read(JsonParser parser)
	{
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				if (key == null) break;
				switch (key)
				{
				case "ip":    ip       = parser.getString();  break;
				case "key":   keyStr   = parser.getString();  break;
				case "ident": ident    = parser.getString();  break;
				case "name":  name     = parser.getString();  break;
				}
				break;
			case VALUE_NUMBER:
				if (key == null) break;
				BigDecimal bd = new BigDecimal(parser.getString());
				switch (key)
				{
				case "beginPort": beginPort = bd.intValue(); break;
				case "endPort":   endPort   = bd.intValue(); break;
				}
				break;
			case END_OBJECT:
				return;
			}
		}
	}

	@Override
	public void print(JsonGenerator generator)
	{
		generator.writeStartObject();
		generator.write("ident", ident);
		generator.write("key", keyStr);
		generator.write("ip", ip);
		generator.write("beginPort", beginPort);
		generator.write("endPort", endPort);
		generator.write("name", name);
		generator.writeEnd();
	}
	
	public RSAPublicKey getKey()
	{
		return KeyPairObject.deSerializePublicKey(keyStr);
	}
	
	public String getKeyStr()
	{
		return keyStr;
	}
	
	public void set(String ident, String key, String ip, int beginPort, int endPort, String name)
	{
		this.ident = ident;
		this.keyStr = key;
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
		this.name = name;
	}

	public String getIdentifer()
	{
		return ident;
	}

	public int getPortEnd()
	{
		return Math.min(endPort, beginPort + 1);
	}

	public int getPortBegin()
	{
		return beginPort;
	}

	public String getIp()
	{
		return ip;
	}

	public String getName()
	{
		return name;
	}

	public String toString()
	{
		return TrackObjectUtils.toString(this);
	}

	public String getAddress()
	{
		return ip + ":" + beginPort + "-" + endPort;
	}

	public void setIp(String realAddress)
	{
		this.ip = realAddress;
	}

	
	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("ident", ident);
		generator.write("keyStr", keyStr);
		generator.write("name", name);
		generator.write("ip", ip);
		generator.write("beginPort", beginPort);
		generator.write("endPort", endPort);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsident = true;
		boolean needskeyStr = true;
		boolean needsname = true;
		boolean needsip = true;
		boolean needsbeginPort = true;
		boolean needsendPort = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsident)
				{
					throw new RuntimeException("Message needs ident");
				}
				if (needskeyStr)
				{
					throw new RuntimeException("Message needs keyStr");
				}
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needsip)
				{
					throw new RuntimeException("Message needs ip");
				}
				if (needsbeginPort)
				{
					throw new RuntimeException("Message needs beginPort");
				}
				if (needsendPort)
				{
					throw new RuntimeException("Message needs endPort");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ident":
				needsident = false;
				ident = parser.getString();
				break;
			case "keyStr":
				needskeyStr = false;
				keyStr = parser.getString();
				break;
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "ip":
				needsip = false;
				ip = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "beginPort":
				needsbeginPort = false;
				beginPort = Integer.parseInt(parser.getString());
				break;
			case "endPort":
				needsendPort = false;
				endPort = Integer.parseInt(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "MachineEntry"; }
	public MachineEntry(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
