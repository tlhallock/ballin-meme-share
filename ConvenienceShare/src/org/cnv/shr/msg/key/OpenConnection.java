package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class OpenConnection extends KeyMessage
{
	public static int TYPE = 0;
	
	private PublicKey sourcePublicKey;
	private PublicKey destinationPublicKey;
	private byte[] requestedNaunce;

	public OpenConnection(InputStream stream) throws IOException
	{
		super(stream);
	}
	public OpenConnection(PublicKey remotePublicKey, byte[] requestedNaunce)
	{
		destinationPublicKey = remotePublicKey;
		this.requestedNaunce = requestedNaunce;
		sourcePublicKey = Services.keyManager.getPublicKey();
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		sourcePublicKey      = reader.readPublicKey();
		destinationPublicKey = reader.readPublicKey();
		requestedNaunce      = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(sourcePublicKey);
		buffer.append(destinationPublicKey);
		buffer.appendVarByteArray(requestedNaunce);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().canAuthenticateRemote(connection, sourcePublicKey, destinationPublicKey))
		{
			connection.getAuthentication().authenticateToTarget(connection, requestedNaunce);
			return;
		}

		PublicKey[] knownKeys = DbKeys.getKeys(connection.getMachine());
		if (knownKeys != null && knownKeys.length > 0)
		{
			LogWrapper.getLogger().info("We have a different key for the remote.");
			connection.send(new KeyNotFound(connection, knownKeys));
			return;
		}
		
		fail("Open connection: has keys, but not claimed keys.", connection);
	}
	
	@Override
	public String toString()
	{
		return "Please open a connection to me. my key=" + sourcePublicKey + " your key= " + destinationPublicKey;
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (sourcePublicKey!=null)
		generator.write("sourcePublicKey", KeyPairObject.serialize(sourcePublicKey));
		if (destinationPublicKey!=null)
		generator.write("destinationPublicKey", KeyPairObject.serialize(destinationPublicKey));
		if (requestedNaunce!=null)
		generator.write("requestedNaunce", Misc.format(requestedNaunce));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needssourcePublicKey = true;
		boolean needsdestinationPublicKey = true;
		boolean needsrequestedNaunce = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needssourcePublicKey)
				{
					throw new RuntimeException("Message needs sourcePublicKey");
				}
				if (needsdestinationPublicKey)
				{
					throw new RuntimeException("Message needs destinationPublicKey");
				}
				if (needsrequestedNaunce)
				{
					throw new RuntimeException("Message needs requestedNaunce");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "sourcePublicKey":
				needssourcePublicKey = false;
				sourcePublicKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "destinationPublicKey":
				needsdestinationPublicKey = false;
				destinationPublicKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "requestedNaunce":
				needsrequestedNaunce = false;
				requestedNaunce = Misc.format(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "OpenConnection"; }
	public OpenConnection(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
