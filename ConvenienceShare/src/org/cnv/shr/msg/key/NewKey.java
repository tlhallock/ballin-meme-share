package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

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
	protected void print(AbstractByteWriter buffer) throws IOException
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
		byte[] newRequest = Services.keyManager.createTestNaunce(connection.getAuthentication(), newKey);
		connection.send(new ConnectionOpenAwk(decrypted, newRequest));
		return;
	}
	
	@Override
	public String toString()
	{
		return "Here is a new key: " + newKey + " with naunce length=" + naunceRequest.length;
	}
}
