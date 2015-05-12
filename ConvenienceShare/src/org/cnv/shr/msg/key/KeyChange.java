package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.ByteListBuffer;

public class KeyChange extends KeyMessage
{
	private PublicKey oldKey;
	private PublicKey newKey;
	private byte[] decryptedProof;
	private byte[] naunceRequest;

	public KeyChange(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	public KeyChange(PublicKey oldKey, PublicKey newKey, byte[] deryptedProof, byte[] naunceRequest)
	{
		this.oldKey = oldKey;
		this.newKey = newKey;
		this.decryptedProof = deryptedProof;
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

	public static final int TYPE = 27;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (getMachine().hasKey(oldKey) && connection.hasPendingNaunce(decryptedProof))
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
