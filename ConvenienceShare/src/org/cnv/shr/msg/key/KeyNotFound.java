package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Map.Entry;
import java.util.Objects;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.IdkWhereToPutThis;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.json.JsonMap;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class KeyNotFound extends KeyMessage
{
	private JsonMap tests = new JsonMap();

	public KeyNotFound(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public KeyNotFound(Communication c, PublicKey[] knownKeys) throws IOException
	{
		for (PublicKey publicKey : knownKeys)
		{
			Objects.requireNonNull(publicKey, "Known keys should not be null.");
			tests.put(publicKey, IdkWhereToPutThis.createTestNaunce(c.getAuthentication(), publicKey));
		}
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		int size = reader.readInt();
		for (int i = 0; i < size; i++)
		{
			PublicKey readPublicKey = reader.readPublicKey();
			byte[] readVarByteArray = reader.readVarByteArray();
			System.out.println(tests);
			tests.put(readPublicKey, readVarByteArray);
		}
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(tests.size());
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			buffer.append(entry.getKey());
			buffer.appendVarByteArray(entry.getValue());
		}
	}

	public static int TYPE = 25;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			PublicKey knownKey = entry.getKey();
			byte[] test = entry.getValue();
			if (Services.keyManager.containsKey(knownKey))
			{
				// able to verify self to remote, but change key
				byte[] decrypted = Services.keyManager.decrypt(knownKey, test);
				// still need to authenticate them.
				byte[] naunceRequest = IdkWhereToPutThis.createTestNaunce(connection.getAuthentication(), 
						connection.getAuthentication().getRemoteKey());
				connection.send(new KeyChange(knownKey, Services.keyManager.getPublicKey(), decrypted, naunceRequest));
				return;
			}
		}
		
		PublicKey localKey = Services.keyManager.getPublicKey();
		connection.getAuthentication().setLocalKey(localKey);
		connection.send(new NewKey(localKey, 
				IdkWhereToPutThis.createTestNaunce(connection.getAuthentication(), 
						connection.getAuthentication().getRemoteKey())));
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Key not found. Known keys are: ");
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			builder.append(Misc.format(entry.getKey().getEncoded()));
			builder.append("->");
			builder.append(Misc.format(entry.getValue()));
			builder.append('\n');
		}
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (tests!=null){
			generator.writeStartArray("tests");
			tests.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needstests = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needstests)
				{
					throw new RuntimeException("Message needs tests");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_ARRAY:
			if (key==null) break;
			if (key.equals("tests")) {
				needstests = false;
				tests.parse(parser);
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "KeyNotFound"; }
	public KeyNotFound(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
