package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

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
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
		newKey = ByteReader.readPublicKey(bytes);
		naunceRequest = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
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
		if (!connection.getAuthentication().newKey(newKey))
		{
			DbMessages.addMessage(new UserMessage.AuthenticationRequest(connection.getMachine(), newKey));
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
}
