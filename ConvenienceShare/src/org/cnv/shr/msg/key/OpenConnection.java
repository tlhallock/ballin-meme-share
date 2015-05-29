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
		if (connection.getAuthentication().canAuthenticateRemote(connection, sourcePublicKey, destinationPublicKey))
		{
			connection.getAuthentication().authenticateToTarget(connection, requestedNaunce);
			return;
		}

		PublicKey[] knownKeys = DbKeys.getKeys(connection.getMachine());
		if (knownKeys != null && knownKeys.length > 0)
		{
			LogWrapper.getLogger().info("We have a different key for the remote.");
			connection.send(new KeyNotFound(connection, knownKeys));
			return;
		}
		
		fail("Open connection: has keys, but not claimed keys.", connection);
	}
	
	@Override
	public String toString()
	{
		return "Please open a connection to me. my key=" + sourcePublicKey + " your key= " + destinationPublicKey;
	}
	
}
