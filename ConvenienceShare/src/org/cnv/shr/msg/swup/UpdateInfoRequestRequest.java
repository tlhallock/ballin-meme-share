package org.cnv.shr.msg.swup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class UpdateInfoRequestRequest extends Message
{
	public static final int TYPE = 37;
	
	public UpdateInfoRequestRequest() {}

	public UpdateInfoRequestRequest(InputStream input) throws IOException
	{
		super(input);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		byte[] pending = Misc.createNaunce(Services.settings.minNaunce.get());
		connection.getAuthentication().addPendingNaunce(pending);
		PublicKey publicKey = Services.updateManager.getPublicKey();
		byte[] encrypted = Services.keyManager.encrypt(publicKey, pending);
		connection.send(new UpdateInfoRequest(publicKey, encrypted));
	}
}
