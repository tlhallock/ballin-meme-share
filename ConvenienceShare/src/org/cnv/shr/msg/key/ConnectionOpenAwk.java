package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ConnectionOpenAwk extends KeyMessage
{
	byte[] decryptedNaunce;
	byte[] naunceRequest;

	public ConnectionOpenAwk(InputStream stream) throws IOException
	{
		super(stream);
	}

	public ConnectionOpenAwk(byte[] encoded, byte[] responseAwk)
	{
		decryptedNaunce = encoded;
		naunceRequest = responseAwk;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		decryptedNaunce = reader.readVarByteArray();
		naunceRequest   = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(decryptedNaunce);
		buffer.appendVarByteArray(naunceRequest);
	}

	public static final int TYPE = 21;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			byte[] decrypted = Services.keyManager.decrypt(connection.getAuthentication().getLocalKey(), naunceRequest);
			connection.send(new ConnectionOpened(decrypted));
			connection.setAuthenticated(true);
		}
		else
		{
			fail("Connection Openned: unable lost pending naunce", connection);
		}
	}
	
	@Override
	public String toString()
	{
		return "You are authenticated too!";
	}
}
