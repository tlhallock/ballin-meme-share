package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;

public class RevokeKey extends Message
{
	private PublicKey revoke;

	public RevokeKey(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer)
	{
		// TODO Auto-generated method stub
		
	}

	public static final int TYPE = 23;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		DbKeys.removeKey(connection.getMachine(), revoke);
	}


	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (revoke!=null)
		generator.write("revoke", KeyPairObject.serialize(revoke));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsrevoke = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsrevoke)
				{
					throw new RuntimeException("Message needs revoke");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("revoke")) {
				needsrevoke = false;
				revoke = KeyPairObject.deSerializePublicKey(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "RevokeKey"; }
	public RevokeKey(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
