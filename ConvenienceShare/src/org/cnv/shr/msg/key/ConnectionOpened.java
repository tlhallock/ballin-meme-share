package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class ConnectionOpened extends Message
{
	byte[] encodedNaunce;
	
	public ConnectionOpened(byte[] encoded)
	{
		this.encodedNaunce = encoded;
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		encodedNaunce = ByteReader.readVarByteArray(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.appendVarByteArray(encodedNaunce);
	}

	public static int TYPE = 10;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (Services.keyManager.confirmPendingNaunce(connection.getRemoteKey(), connection.getPendingNaunce(), encodedNaunce))
		{
			connection.isAuthenticated();
		}
		else
		{
			connection.send(new KeyFailure());
			connection.notifyDone();
		}
	}
}
