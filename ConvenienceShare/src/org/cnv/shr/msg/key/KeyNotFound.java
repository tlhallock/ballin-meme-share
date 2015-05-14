package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map.Entry;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class KeyNotFound extends KeyMessage
{
	HashMap<PublicKey, byte[]> tests = new HashMap<>();

	public KeyNotFound(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public KeyNotFound(Communication c, PublicKey[] knownKeys) throws IOException
	{
		for (PublicKey publicKey : knownKeys)
		{
			if (publicKey == null)
			{
				throw new NullPointerException("Known keys should not be null.");
			}
			tests.put(publicKey, Services.keyManager.createTestNaunce(c, publicKey));
		}
	}

	@Override
	public void parse(InputStream bytes) throws IOException
	{
		int size = ByteReader.readInt(bytes);
		for (int i = 0; i < size; i++)
		{
			PublicKey readPublicKey = ByteReader.readPublicKey(bytes);
			byte[] readVarByteArray = ByteReader.readVarByteArray(bytes);
			System.out.println(tests);
			tests.put(readPublicKey, readVarByteArray);
		}
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(tests.size());
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			buffer.append(entry.getKey());
			buffer.appendVarByteArray(entry.getValue());
		}
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
		
		PublicKey localKey = Services.keyManager.getPublicKey();
		connection.setLocalKey(localKey);
		connection.send(new NewKey(localKey, 
				Services.keyManager.createTestNaunce(connection, connection.getRemoteKey())));
	}

	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Key not found. Known keys are: ");
		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
		{
			builder.append(Misc.format(entry.getKey().getEncoded()));
			builder.append("->");
			builder.append(Misc.format(entry.getValue()));
			builder.append('\n');
		}
		return builder.toString();
	}
}
