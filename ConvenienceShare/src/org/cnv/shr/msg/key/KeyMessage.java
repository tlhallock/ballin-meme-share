package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;

import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.LogWrapper;

public abstract class KeyMessage extends Message
{
	protected KeyMessage() {}

	protected KeyMessage(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	public boolean requiresAthentication()
	{
		return false;
	}
	
	public void fail(String message, Communication connection) throws IOException
	{
		try
		{
			connection.send(new KeyFailure(message));
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			connection.setAuthenticated(false);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "No provider", e);
			Services.quiter.quit();
		}
		connection.finish();
	}
}
