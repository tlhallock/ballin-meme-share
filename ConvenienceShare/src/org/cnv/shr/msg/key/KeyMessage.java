package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

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
}
