package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class InitiateAuthentication extends Message
{
	public static int TYPE = 0;
	
	PublicKey sourcePublicKey;
	PublicKey destinationPublicKey;
	byte[] requestedNaunce;
	
	public InitiateAuthentication(PublicKey remotePublicKey, byte[] requestedNaunce)
	{
		destinationPublicKey = remotePublicKey;
		this.requestedNaunce = requestedNaunce;
		sourcePublicKey = Services.keyManager.getPublicKey();
	}
	
	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		sourcePublicKey      = ByteReader.readPublicKey(bytes);
		destinationPublicKey = ByteReader.readPublicKey(bytes);
		requestedNaunce      = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
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
	
	private boolean authenticateRemote(Communication connection)
	{
		// authenticate remote...
		if (getMachine().hasKey(sourcePublicKey))
		{
			return true;
		}

		if (Services.keyManager.acceptKey(getMachine(), sourcePublicKey))
		{
			destinationPublicKey = Services.keyManager.getPublicKey();
			DbKeys.addKey(getMachine(), sourcePublicKey);
			return true;
		}
	
		PublicKey[] knownKeys = DbKeys.getKeys(getMachine());
		if (knownKeys != null)
		{
			connection.send(new KeyNotFound(knownKeys, knownKeys));
			return false;
		}

		// add message
		connection.send(new KeyFailure());
		connection.notifyDone();
		return false;
	}
}
