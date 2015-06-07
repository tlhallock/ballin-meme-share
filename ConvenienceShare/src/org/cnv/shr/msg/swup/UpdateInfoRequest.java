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
import org.cnv.shr.util.Misc;

public class UpdateInfoRequest extends Message
{
	public static final int TYPE = 38;
	
	private PublicKey publicKey;
	private byte[] naunceRequest;
	
	public UpdateInfoRequest(InputStream input) throws IOException
	{
		super(input);
	}
	
	public UpdateInfoRequest(PublicKey pKey, byte[] encrypted)
	{
		this.publicKey = pKey;
		this.naunceRequest = encrypted;
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		naunceRequest = reader.readVarByteArray();
		publicKey = reader.readPublicKey();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(naunceRequest);
		buffer.append(publicKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (Services.codeUpdateInfo == null)
		{
			connection.setDone();
			return;
		}
		byte[] decrypted = Services.keyManager.decrypt(Services.codeUpdateInfo.getPrivateKey(publicKey), naunceRequest);
		connection.send(new UpdateInfoMessage(decrypted));
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (publicKey!=null)
		generator.write("publicKey", KeyPairObject.serialize(publicKey));
		if (naunceRequest!=null)
		generator.write("naunceRequest", Misc.format(naunceRequest));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needspublicKey = true;
		boolean needsnaunceRequest = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needspublicKey)
				{
					throw new RuntimeException("Message needs publicKey");
				}
				if (needsnaunceRequest)
				{
					throw new RuntimeException("Message needs naunceRequest");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "publicKey":
				needspublicKey = false;
				publicKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "naunceRequest":
				needsnaunceRequest = false;
				naunceRequest = Misc.format(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "UpdateInfoRequest"; }
	public UpdateInfoRequest(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
