package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.msg.Message;

public abstract class KeyMessage extends Message
{
	protected KeyMessage()
	{
		
	}
	
	protected KeyMessage(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}

	public boolean requiresAthentication()
	{
		return false;
	}
}
