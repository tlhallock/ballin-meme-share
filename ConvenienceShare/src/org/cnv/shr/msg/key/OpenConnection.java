package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbKeys;
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
	protected void parse(ByteReader reader) throws IOException
	{
		sourcePublicKey      = reader.readPublicKey();
		destinationPublicKey = reader.readPublicKey();
		requestedNaunce      = reader.readVarByteArray();
	}

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
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
		Machine machine = connection.getMachine();
		if (connection.getAuthentication().canAuthenticateRemote(machine, sourcePublicKey, destinationPublicKey))
		{
			connection.getAuthentication().authenticateToTarget(connection, requestedNaunce);
			return;
		}
		
		PublicKey[] knownKeys = DbKeys.getKeys(machine);
		if (knownKeys != null && knownKeys.length > 0)
		{
			Services.logger.println("We have a different key for the remote.");
			connection.send(new KeyNotFound(connection, knownKeys));
			return;
		}
		
		fail("Open connection: has keys, but not claimed keys.", connection);
	}
	
	public String toString()
	{
		return "Please open a connection to me. my key=" + sourcePublicKey + " your key= " + destinationPublicKey;
	}
	
}
