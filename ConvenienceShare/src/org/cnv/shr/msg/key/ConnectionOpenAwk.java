package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class ConnectionOpenAwk extends Message
{
	byte[] encrtypedNaunce;
	byte[] naunceRequest;

	public ConnectionOpenAwk(byte[] encoded, byte[] responseAwk)
	{
		encrtypedNaunce = encoded;
		naunceRequest = responseAwk;
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		encrtypedNaunce = ByteReader.readVarByteArray(bytes);
		naunceRequest   = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.appendVarByteArray(encrtypedNaunce);
		buffer.appendVarByteArray(naunceRequest);
	}

	public static final int TYPE = 0;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (Services.keyManager.confirmPendingNaunce(
				connection.getRemoteKey(),
				connection.getPendingNaunce(),
				encrtypedNaunce))
		{
			byte[] encoded = Services.keyManager.encode(connection.getLocalKey(), naunceRequest);
			connection.send(new ConnectionOpened(encoded));
			connection.isAuthenticated();
		}
		else
		{
			connection.send(new KeyFailure());
			connection.notifyDone();
		}
	}
}
