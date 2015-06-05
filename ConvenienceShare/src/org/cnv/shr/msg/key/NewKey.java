package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.IdkWhereToPutThis;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class NewKey extends KeyMessage
{
	PublicKey newKey;
	byte[] naunceRequest;

	public NewKey(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public NewKey(PublicKey publicKey, byte[] responseAwk)
	{
		this.newKey = publicKey;
		naunceRequest = responseAwk;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		newKey = reader.readPublicKey();
		naunceRequest = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(newKey);
		buffer.appendVarByteArray(naunceRequest);
	}

	public static int TYPE = 22;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (!connection.getAuthentication().newKey(connection, newKey) || naunceRequest.length == 0)
		{
//			DbMessages.addMessage(new UserMessage.AuthenticationRequest(connection.getMachine(), newKey));
                        
                        
			LogWrapper.getLogger().info("We have no naunce to authenticate!");
			fail("New key not accepted.", connection);
			return;
		}
		
		DbKeys.addKey(connection.getMachine(), newKey);
		connection.getAuthentication().setRemoteKey(newKey);

		byte[] decrypted = Services.keyManager.decrypt(connection.getAuthentication().getLocalKey(), naunceRequest);
		byte[] newRequest = IdkWhereToPutThis.createTestNaunce(connection.getAuthentication(), newKey);
		connection.send(new ConnectionOpenAwk(decrypted, newRequest));
		return;
	}
	
	@Override
	public String toString()
	{
		return "Here is a new key: " + newKey + " with naunce length=" + naunceRequest.length;
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("newKey", KeyPairObject.serialize(newKey));
		generator.write("naunceRequest", Misc.format(naunceRequest));
		generator.writeEnd();
	}

	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("newKey")) {
				newKey = JsonThing.readKey(parser);
			}
			break;
			default: break;
			}
		}
	}
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
