package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class ConnectionOpenAwk extends KeyMessage
{
	byte[] decryptedNaunce;
	byte[] naunceRequest;

	public ConnectionOpenAwk(InputStream stream) throws IOException
	{
		super(stream);
	}

	public ConnectionOpenAwk(byte[] encoded, byte[] responseAwk)
	{
		decryptedNaunce = encoded;
		naunceRequest = responseAwk;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		decryptedNaunce = reader.readVarByteArray();
		naunceRequest   = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(decryptedNaunce);
		buffer.appendVarByteArray(naunceRequest);
	}

	public static final int TYPE = 21;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			byte[] decrypted = Services.keyManager.decrypt(connection.getAuthentication().getLocalKey(), naunceRequest);
			connection.send(new ConnectionOpened(decrypted));
			connection.setAuthenticated(true);
		}
		else
		{
			fail("Connection Openned: unable lost pending naunce", connection);
		}
	}
	
	@Override
	public String toString()
	{
		return "You are authenticated too!";
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("decryptedNaunce", Misc.format(decryptedNaunce));
		generator.write("naunceRequest", Misc.format(naunceRequest));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdecryptedNaunce = true;
		boolean needsnaunceRequest = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdecryptedNaunce)
				{
					throw new RuntimeException("Message needs decryptedNaunce");
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
			case "decryptedNaunce":
				needsdecryptedNaunce = false;
				decryptedNaunce = Misc.format(parser.getString());
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
	public String getJsonName() { return "ConnectionOpenAwk"; }
	public ConnectionOpenAwk(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
