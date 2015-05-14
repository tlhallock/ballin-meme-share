package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

import de.flexiprovider.core.rijndael.RijndaelKey;

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
	public void parse(InputStream bytes) throws IOException
	{
		decryptedNaunce = ByteReader.readVarByteArray(bytes);
		naunceRequest   = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
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
			RijndaelKey aesKey = Services.keyManager.createAesKey();
			byte[] decrypted = Services.keyManager.decryptNaunce(connection.getLocalKey(), naunceRequest);
			connection.send(new ConnectionOpened(aesKey, decrypted));
			connection.notifyAuthentication(true, aesKey);
		}
		else
		{
			connection.notifyAuthentication(false, null);
			connection.send(new KeyFailure("ConnectionOpenAwk: first naunce failed."));
			connection.notifyDone();
		}
	}
}
