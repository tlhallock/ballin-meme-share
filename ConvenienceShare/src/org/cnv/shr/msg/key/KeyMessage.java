package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;

public abstract class KeyMessage extends Message
{
	protected KeyMessage() {}

	protected KeyMessage(InputStream stream) throws IOException
	{
		super(stream);
	}

	public boolean requiresAthentication()
	{
		return false;
	}
	
	public void fail(Communication connection)
	{
		connection.send(new KeyFailure("ConnectionOpenAwk: first naunce failed."));
		try
		{
			connection.setAuthenticated(null);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e)
		{
			Services.logger.print(e);
			Main.quit();
		}
		connection.finish();
	}
}
