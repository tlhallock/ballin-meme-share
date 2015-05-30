package org.cnv.shr.msg.swup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class UpdateInfoMessage extends Message
{
	public static int TYPE = 39;
	
	private String ip;
	private int port;
	private PublicKey pKey;
	byte[]  decryptedNaunce;

	public UpdateInfoMessage(byte[] decrypted)
	{
		decryptedNaunce = decrypted;
		ip = Services.codeUpdateInfo.getIp();
		port = Services.codeUpdateInfo.getPort();
		pKey = Services.codeUpdateInfo.getLatestPublicKey();
	}
	
	public UpdateInfoMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		ip = reader.readString();
		port = reader.readInt();
		pKey = reader.readPublicKey();
		decryptedNaunce = reader.readVarByteArray();
	}
	
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(pKey);
		buffer.appendVarByteArray(decryptedNaunce);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (!connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			LogWrapper.getLogger().info("Update server machine failed authentication.");
			return;
		}
		
		Services.updateManager.updateInfo(ip, port, pKey);
		Services.updateManager.checkForUpdates();
	}
}
