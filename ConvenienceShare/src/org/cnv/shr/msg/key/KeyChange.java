package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.db.h2.DbKeys;
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
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
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
		buffer.appendVarByteArray(decryptedProof);
		buffer.appendVarByteArray(naunceRequest );
		
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
		if (DbKeys.machineHasKey(machine, oldKey) && connection.getAuthentication().hasPendingNaunce(decryptedProof))
		{
			DbKeys.addKey(machine, newKey);
			connection.getAuthentication().setRemoteKey(newKey);
			connection.getAuthentication().authenticateToTarget(connection, naunceRequest);
			return;
		}
		fail("Key change: did not know key.", connection);
	}
}
