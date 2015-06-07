package org.cnv.shr.msg.swup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class UpdateInfoMessage extends Message
{
	public static int TYPE = 39;
	
	private String ip;
	private int port;
	private PublicKey pKey;
	byte[]  decryptedNaunce;

	public UpdateInfoMessage(byte[] decrypted)
	{
		decryptedNaunce = decrypted;
		ip = Services.codeUpdateInfo.getIp();
		port = Services.codeUpdateInfo.getPort();
		pKey = Services.codeUpdateInfo.getLatestPublicKey();
	}
	
	public UpdateInfoMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		ip = reader.readString();
		port = reader.readInt();
		pKey = reader.readPublicKey();
		decryptedNaunce = reader.readVarByteArray();
	}
	
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(pKey);
		buffer.appendVarByteArray(decryptedNaunce);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (!connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			LogWrapper.getLogger().info("Update server machine failed authentication.");
			return;
		}
		
		Services.updateManager.updateInfo(ip, port, pKey);
		Services.updateManager.checkForUpdates(null, true);
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (ip!=null)
		generator.write("ip", ip);
		generator.write("port", port);
		if (pKey!=null)
		generator.write("pKey", KeyPairObject.serialize(pKey));
		if (decryptedNaunce!=null)
		generator.write("decryptedNaunce", Misc.format(decryptedNaunce));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsip = true;
		boolean needspKey = true;
		boolean needsdecryptedNaunce = true;
		boolean needsport = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsip)
				{
					throw new RuntimeException("Message needs ip");
				}
				if (needspKey)
				{
					throw new RuntimeException("Message needs pKey");
				}
				if (needsdecryptedNaunce)
				{
					throw new RuntimeException("Message needs decryptedNaunce");
				}
				if (needsport)
				{
					throw new RuntimeException("Message needs port");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ip":
				needsip = false;
				ip = parser.getString();
				break;
			case "pKey":
				needspKey = false;
				pKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "decryptedNaunce":
				needsdecryptedNaunce = false;
				decryptedNaunce = Misc.format(parser.getString());
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("port")) {
				needsport = false;
				port = Integer.parseInt(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "UpdateInfoMessage"; }
	public UpdateInfoMessage(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
