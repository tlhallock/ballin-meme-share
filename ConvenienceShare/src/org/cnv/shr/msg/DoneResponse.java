package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;

public class DoneResponse extends Message
{
	public DoneResponse() {}
	
	public DoneResponse(InputStream input) throws IOException
	{
		super(input);
	}

	public static int TYPE = 31;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
//		for (int i = 0; i < DoneMessage.DONE_PADDING; i++)
//		{
//			buffer.append(0);
//		}
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Services.networkManager.closeConnection(connection);
	}

	public boolean requiresAthentication()
	{
		return true;
	}
	
	public String toString()
	{
		return "I am done too.";
	}
}
