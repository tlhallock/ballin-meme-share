package org.cnv.shr.msg.swup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class UpdateInfoRequest extends Message
{
	public static final int TYPE = 38;
	
	private PublicKey publicKey;
	private byte[] naunceRequest;
	
	public UpdateInfoRequest(InputStream input) throws IOException
	{
		super(input);
	}
	
	public UpdateInfoRequest(PublicKey pKey, byte[] encrypted)
	{
		this.publicKey = pKey;
		this.naunceRequest = encrypted;
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		naunceRequest = reader.readVarByteArray();
		publicKey = reader.readPublicKey();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(naunceRequest);
		buffer.append(publicKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (Services.codeUpdateInfo == null)
		{
			connection.setDone();
			return;
		}
		byte[] decrypted = Services.keyManager.decrypt(Services.codeUpdateInfo.getPrivateKey(publicKey), naunceRequest);
		connection.send(new UpdateInfoMessage(decrypted));
		
	}
}
