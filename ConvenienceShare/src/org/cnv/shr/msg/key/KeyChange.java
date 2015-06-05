package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.Misc;

public class KeyChange extends KeyMessage
{
	private PublicKey oldKey;
	private PublicKey newKey;
	private byte[] decryptedProof;
	private byte[] naunceRequest;

	public KeyChange(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public KeyChange(PublicKey oldKey, PublicKey newKey, byte[] deryptedProof, byte[] naunceRequest)
	{
		this.oldKey = oldKey;
		this.newKey = newKey;
		this.decryptedProof = deryptedProof;
		this.naunceRequest = naunceRequest;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		oldKey         = reader.readPublicKey();
		newKey         = reader.readPublicKey();
		decryptedProof = reader.readVarByteArray();
		naunceRequest  = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(oldKey        );
		buffer.append(newKey        );
		buffer.appendVarByteArray(decryptedProof);
		buffer.appendVarByteArray(naunceRequest );
		
	}

	public static final int TYPE = 27;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine machine = connection.getMachine();
		if (DbKeys.machineHasKey(machine, oldKey) && connection.getAuthentication().hasPendingNaunce(decryptedProof))
		{
			DbKeys.addKey(machine, newKey);
			connection.getAuthentication().setRemoteKey(newKey);
			connection.getAuthentication().authenticateToTarget(connection, naunceRequest);
			return;
		}
		fail("Key change: did not know key.", connection);
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("oldKey", KeyPairObject.serialize(oldKey));
		generator.write("newKey", KeyPairObject.serialize(newKey));
		generator.write("decryptedProof", Misc.format(decryptedProof));
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
			switch(key) {
			case "oldKey":
				oldKey = JsonThing.readKey(parser);
				break;
			case "newKey":
				newKey = JsonThing.readKey(parser);
				break;
			}
			break;
			default: break;
			}
		}
	}
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
