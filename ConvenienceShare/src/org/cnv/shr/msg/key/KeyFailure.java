package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;

public class KeyFailure extends KeyMessage
{
	private String reason;
	
	public KeyFailure(String reason)
	{
		this.reason = reason;
	}
	public KeyFailure(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	public void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}

	public static int TYPE = 26;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		return "Unable to authenticate: " + reason;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Services.logger.logStream.println("Key failure");
		connection.notifyAuthentication(false, null);
		connection.notifyDone();
	}
}
