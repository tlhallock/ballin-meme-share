package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class ConnectionOpenAwk extends KeyMessage
{
	byte[] decryptedNaunce;
	byte[] naunceRequest;

	public ConnectionOpenAwk(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}

	public ConnectionOpenAwk(byte[] encoded, byte[] responseAwk)
	{
		decryptedNaunce = encoded;
		naunceRequest = responseAwk;
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		decryptedNaunce = ByteReader.readVarByteArray(bytes);
		naunceRequest   = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
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
		if (connection.hasPendingNaunce(decryptedNaunce))
		{
			byte[] decrypted = Services.keyManager.decryptNaunce(connection.getLocalKey(), naunceRequest);
			connection.send(new ConnectionOpened(decrypted));
			connection.notifyAuthentication(true);
		}
		else
		{
			connection.notifyAuthentication(false);
			connection.send(new KeyFailure());
			connection.notifyDone();
		}
	}
}
