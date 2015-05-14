package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class WhoIAm extends MachineFound
{
	public static int TYPE = 29;
	
	protected java.security.PublicKey[] keys;
	
	public WhoIAm(InputStream input) throws IOException
	{
		super(input);
	}
	
	public WhoIAm()
	{
		super();
		keys       = new PublicKey[] {Services.keyManager.getPublicKey()};
	}
	
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void parse(InputStream bytes) throws IOException
	{
		super.parse(bytes);
		int numKeys = ByteReader.readInt(bytes);
		keys = new PublicKey[numKeys];
		for (int i = 0; i < numKeys; i++)
		{
			keys[i] = ByteReader.readPublicKey(bytes);
		}
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		super.write(buffer);
		buffer.append(keys.length);
		for (PublicKey key : keys)
		{
			buffer.append(key);
		}
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.offerMachine(createMachine(), keys);
	}

	public boolean requiresAthentication()
	{
		return false;
	}
}
