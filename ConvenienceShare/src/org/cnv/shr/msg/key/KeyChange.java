package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class KeyChange extends KeyMessage
{
	private PublicKey oldKey;
	private PublicKey newKey;
	private byte[] decryptedProof;
	private byte[] naunceRequest;

	public KeyChange(InputStream stream) throws IOException
	{
		super(stream);
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
		oldKey         = ByteReader.readPublicKey(bytes);
		newKey         = ByteReader.readPublicKey(bytes);
		decryptedProof = ByteReader.readVarByteArray(bytes);
		naunceRequest  = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(oldKey        );
		buffer.append(newKey        );
		buffer.append(decryptedProof);
		buffer.append(naunceRequest );
		
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
		Machine machine = connection.getMachine();
		if (machine.hasKey(oldKey) && connection.hasPendingNaunce(decryptedProof))
		{
			DbKeys.addKey(machine, newKey);
			connection.updateKey(newKey);
			connection.authenticateToTarget(naunceRequest);
			return;
		}
		connection.send(new KeyFailure());
		connection.notifyDone();
	}
}
