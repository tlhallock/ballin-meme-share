package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map.Entry;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class KeyNotFound extends KeyMessage
{
	HashMap<PublicKey, byte[]> tests = new HashMap<>();

	public KeyNotFound(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	public KeyNotFound(Communication c, PublicKey[] knownKeys) throws IOException
	{
		for (PublicKey publicKey : knownKeys)
		{
			tests.put(publicKey, Services.keyManager.createTestNaunce(c, publicKey));
		}
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		
	}

	public static int TYPE = 25;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			PublicKey knownKey = entry.getKey();
			byte[] test = entry.getValue();
			if (Services.keyManager.containsKey(knownKey))
			{
				// able to verify self to remote, but change key
				byte[] decrypted = Services.keyManager.decryptNaunce(knownKey, test);
				// still need to authenticate them.
				byte[] naunceRequest = Services.keyManager.createTestNaunce(connection, connection.getRemoteKey());
				connection.send(new KeyChange(knownKey, Services.keyManager.getPublicKey(), decrypted, naunceRequest));
				return;
			}
		}

		connection.send(new NewKey(Services.keyManager.getPublicKey(), 
				Services.keyManager.createTestNaunce(connection, connection.getRemoteKey())));
	}

}
