package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map.Entry;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.Misc;

public class KeyNotFound extends Message
{
	HashMap<PublicKey, byte[]> tests = new HashMap<>();
	byte[] requestedNaunce;

	public KeyNotFound(PublicKey[] requestedNaunces)
	{
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
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			PublicKey knownKey = entry.getKey();
			byte[] test = entry.getValue();
			if (Services.keyManager.containsKey(knownKey))
			{
				// able to verify self to remote, but change key
				byte[] decrypted = Services.keyManager.decryptNaunce(knownKey, test);
				// need to know their public key
				byte[] responseAwk = Misc.getNaunce();
				connection.addPendingNaunce(responseAwk);
				connection.send(new KeyChange(knownKey, Services.keyManager.getPublicKey(), decrypted, responseAwk));
				return;
			}
		}

		connection.send(new KeyFailure());
		connection.notifyDone();
	}

}
