package org.cnv.shr.msg.dwn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

import de.flexiprovider.core.rijndael.RijndaelKey;

public class NewAesKey extends Message
{
	public static final int TYPE = 34;
	
	private byte[] encryptedAesKey;
	
	public NewAesKey(InputStream input) throws IOException
	{
		super(input);
	}
	
	public NewAesKey(RijndaelKey aesKey, PublicKey pKey) throws IOException
	{
		Services.keyManager.encrypt(pKey, aesKey.getEncoded());
		this.encryptedAesKey = getBytes(pKey, aesKey);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		encryptedAesKey = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(encryptedAesKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.decrypt(getKey(connection.getAuthentication().getLocalKey(), encryptedAesKey));
	}
	
	private static RijndaelKey getKey(PublicKey pKey, byte[] bytes) throws IOException, ClassNotFoundException
	{
		try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(
				Services.keyManager.decrypt(pKey, bytes)));)
		{
			return (RijndaelKey) objectInputStream.readObject();
		}
	}
	
	private static byte[] getBytes(PublicKey key, RijndaelKey aesKey) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);)
		{
			objectOutputStream.writeObject(aesKey);
		}
		return Services.keyManager.encrypt(key, byteArrayOutputStream.toByteArray());
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("encryptedAesKey", Misc.format(encryptedAesKey));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsencryptedAesKey = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsencryptedAesKey)
				{
					throw new RuntimeException("Message needs encryptedAesKey");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("encryptedAesKey")) {
				needsencryptedAesKey = false;
				encryptedAesKey = Misc.format(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "NewAesKey"; }
	public NewAesKey(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
