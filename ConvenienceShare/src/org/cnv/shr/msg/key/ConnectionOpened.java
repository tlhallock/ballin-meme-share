package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class ConnectionOpened extends KeyMessage
{
	byte[] decryptedNaunce;

	public ConnectionOpened(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	public ConnectionOpened(byte[] encoded)
	{
		this.decryptedNaunce = encoded;
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		decryptedNaunce = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.appendVarByteArray(decryptedNaunce);
	}

	public static int TYPE = 24;
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
			connection.notifyAuthentication(true);
		}
		else
		{
			connection.send(new KeyFailure());
			connection.notifyDone();
			connection.notifyAuthentication(false);
		}
	}
}
