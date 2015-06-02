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
	
	private String ip;
	private int beginPort;
	private int endPort;
	
	public MachineEntry() {}

	public MachineEntry(String ident, RSAPublicKey key, String ip, int beginPort, int endPort)
	{
		this(ident, KeyPairObject.serialize(key), ip, beginPort, endPort);
	}
	
	public MachineEntry(String ident, String key, String ip, int beginPort, int endPort)
	{
		this.ident = ident;
		this.keyStr = key;
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
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
	
	public void set(String ident, String key, String ip, int beginPort, int endPort)
	{
		this.ident = ident;
		this.keyStr = key;
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
	}

	public String getIdentifer()
	{
		return ident;
	}

	public int getPortEnd()
	{
		return endPort;
	}

	public int getPortBegin()
	{
		return beginPort;
	}

	public String getIp()
	{
		return ip;
	}
	
	public String toString()
	{
		return TrackObjectUtils.toString(this);
	}
}
