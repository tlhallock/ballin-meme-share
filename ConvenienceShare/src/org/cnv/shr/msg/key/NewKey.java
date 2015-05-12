package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class NewKey extends Message
{
	PublicKey newKey;
	byte[] encryptedNaunce;
	byte[] naunceRequest;

	public NewKey(PublicKey publicKey, byte[] encoded, byte[] responseAwk)
	{
		this.newKey = publicKey;
		this.encryptedNaunce = encoded;
		naunceRequest = responseAwk;
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int getType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (Services.keyManager.acceptKey(getMachine(), newKey)
				&& Services.keyManager.confirmPendingNaunce(newKey, connection.getPendingNaunce(), encryptedNaunce))
		{
			DbKeys.addKey(getMachine(), newKey);
			connection.updateKey(newKey);
			connection.authenticateToTarget(naunceRequest);
			
//			connection.updateKey(newKey);
//			DbKeys.addKey(getMachine(), newKey);
//			
//			byte[] encoded = Services.keyManager.encode(newKey, naunceRequest);
//			connection.send(new ConnectionOpened(encoded));
			return;
		}
		
		// add message
		connection.send(new KeyFailure());
		connection.notifyDone();
	}
}
