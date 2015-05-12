package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class KeyChange extends Message
{
	public static int TYPE = 1;
	
	PublicKey oldKey;
	PublicKey newKey;
	byte[] deryptedProof;
	byte[] naunceRequest;

	public KeyChange(PublicKey oldKey, PublicKey newKey, byte[] deryptedProof, byte[] naunceRequest)
	{
		this.oldKey = oldKey;
		this.newKey = newKey;
		this.deryptedProof = deryptedProof;
		this.naunceRequest = naunceRequest;
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
		return 0;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (       (getMachine().hasKey(oldKey)                         && Services.keyManager.confirmPendingNaunce(oldKey, connection.getPendingNaunce(), encryptedNaunce))
				|| (Services.keyManager.acceptKey(getMachine(), newKey) && Services.keyManager.confirmPendingNaunce(newKey, connection.getPendingNaunce(), encryptedNaunce)))
		{
			DbKeys.addKey(getMachine(), newKey);
			connection.updateKey(newKey);
			connection.authenticateToTarget(naunceRequest);
			return;
		}
		connection.send(new KeyFailure());
		connection.notifyDone();
	}
}
