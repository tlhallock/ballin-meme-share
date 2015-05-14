package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class OpenConnection extends KeyMessage
{
	public static int TYPE = 0;
	
	private PublicKey sourcePublicKey;
	private PublicKey destinationPublicKey;
	private byte[] requestedNaunce;

	public OpenConnection(InputStream stream) throws IOException
	{
		super(stream);
	}
	public OpenConnection(PublicKey remotePublicKey, byte[] requestedNaunce)
	{
		destinationPublicKey = remotePublicKey;
		this.requestedNaunce = requestedNaunce;
		sourcePublicKey = Services.keyManager.getPublicKey();
	}
	
	@Override
	public void parse(InputStream bytes) throws IOException
	{
		sourcePublicKey      = ByteReader.readPublicKey(bytes);
		destinationPublicKey = ByteReader.readPublicKey(bytes);
		requestedNaunce      = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(sourcePublicKey);
		buffer.append(destinationPublicKey);
		buffer.appendVarByteArray(requestedNaunce);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.setKeys(sourcePublicKey, destinationPublicKey);
		if (!authenticateRemote(connection))
		{
			return;
		}
		connection.authenticateToTarget(requestedNaunce);
	}
	
	private boolean authenticateRemote(Communication connection) throws IOException
	{
		// authenticate remote...
		Machine machine = connection.getMachine();
		if (DbKeys.machineHasKey(machine, sourcePublicKey))
		{
			return true;
		}

		if (connection.acceptKey(sourcePublicKey))
		{
			DbKeys.addKey(machine, sourcePublicKey);
			return true;
		}
	
		PublicKey[] knownKeys = DbKeys.getKeys(machine);
		if (knownKeys != null && knownKeys.length > 0)
		{
			connection.send(new KeyNotFound(connection, knownKeys));
			return false;
		}

		// add message
		connection.send(new KeyFailure("Open connection authenticate to remote: no known keys available."));
		connection.notifyDone();
		return false;
	}
}
