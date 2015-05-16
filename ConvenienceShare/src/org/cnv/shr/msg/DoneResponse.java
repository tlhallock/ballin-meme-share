package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

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
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.setDone();
		connection.getSocket().close();
	}

	public boolean requiresAthentication()
	{
		return false;
	}
	
	public String toString()
	{
		return "I am done too.";
	}
}
