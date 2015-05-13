package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;

public class NewKey extends Message
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
	protected void parse(InputStream bytes) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void write(AbstractByteWriter buffer)
	{
		// TODO Auto-generated method stub
		
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
		if (!connection.acceptKey(newKey))
		{
			DbMessages.addMessage(new UserMessage.AuthenticationRequest(connection.getMachine(), newKey));
			connection.send(new KeyFailure());
			connection.notifyAuthentication(false);
			connection.notifyDone();
		}
		
		DbKeys.addKey(connection.getMachine(), newKey);
		connection.updateKey(newKey);

		byte[] decrypted = Services.keyManager.decryptNaunce(connection.getLocalKey(), naunceRequest);
		byte[] newRequest = Services.keyManager.createTestNaunce(connection, newKey);
		connection.send(new ConnectionOpenAwk(decrypted, newRequest));
		return;
	}
}
