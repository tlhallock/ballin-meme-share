package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class ConnectionOpened extends KeyMessage
{
	private byte[] decryptedNaunce;
	
	public ConnectionOpened(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public ConnectionOpened(byte[] encoded) throws IOException
	{
		this.decryptedNaunce = encoded;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		decryptedNaunce = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(decryptedNaunce);
	}

	public static int TYPE = 24;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		return "You are authenticated.";
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			connection.setAuthenticated(true);
		}
		else
		{
			fail("Connection opened: failed first naunce.", connection);
		}
	}
	
	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (decryptedNaunce!=null)
		generator.write("decryptedNaunce", Misc.format(decryptedNaunce));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdecryptedNaunce = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdecryptedNaunce)
				{
					throw new RuntimeException("Message needs decryptedNaunce");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("decryptedNaunce")) {
				needsdecryptedNaunce = false;
				decryptedNaunce = Misc.format(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "ConnectionOpened"; }
	public ConnectionOpened(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
