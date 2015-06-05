package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;

public class WhoIAm extends MachineFound
{
	public static int TYPE = 29;
	
	protected PublicKey pKey;
	protected String versionString;
	
	public WhoIAm(InputStream input) throws IOException
	{
		super(input);
	}
	
	public WhoIAm()
	{
		super();
		pKey       = Services.keyManager.getPublicKey();
		versionString = "0.0.1";
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		super.parse(reader);
		versionString = reader.readString();
		pKey = reader.readPublicKey();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		super.print(connection, buffer);
		buffer.append(versionString);
		buffer.append(pKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.setRemoteIdentifier(ident);
		connection.getAuthentication().setMachineInfo(name, port, nports);
		connection.getAuthentication().offerRemote(ident, connection.getIp(), pKey);
	}

	@Override
	public boolean requiresAthentication()
	{
		return false;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("I am a machine with ident=" + ident + " on a port " + port);
		return builder.toString();
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("pKey", KeyPairObject.serialize(pKey));
		generator.write("versionString", versionString);
		generator.write("port", port);
		generator.write("nports", nports);
		generator.write("name", name);
		generator.write("ident", ident);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needspKey = true;
		boolean needsversionString = true;
		boolean needsname = true;
		boolean needsident = true;
		boolean needsport = true;
		boolean needsnports = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needspKey)
				{
					throw new RuntimeException("Message needs pKey");
				}
				if (needsversionString)
				{
					throw new RuntimeException("Message needs versionString");
				}
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needsident)
				{
					throw new RuntimeException("Message needs ident");
				}
				if (needsport)
				{
					throw new RuntimeException("Message needs port");
				}
				if (needsnports)
				{
					throw new RuntimeException("Message needs nports");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "pKey":
				needspKey = false;
				pKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "versionString":
				needsversionString = false;
				versionString = parser.getString();
				break;
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "ident":
				needsident = false;
				ident = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "port":
				needsport = false;
				port = Integer.parseInt(parser.getString());
				break;
			case "nports":
				needsnports = false;
				nports = Integer.parseInt(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "WhoIAm"; }
	public WhoIAm(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
