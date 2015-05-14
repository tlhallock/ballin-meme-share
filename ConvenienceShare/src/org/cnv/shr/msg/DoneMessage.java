package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.AbstractByteWriter;

public class DoneMessage extends Message
{
	public static int TYPE = 1;
	public static int DONE_PADDING = 20;
	
	public DoneMessage() {}
	public DoneMessage(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	public void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		for (int i = 0; i < DONE_PADDING; i++)
		{
			buffer.append(0);
		}
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.remoteIsDone();
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("done.");
		
		return builder.toString();
	}
	
	public boolean requiresAthentication()
	{
		return false;
	}
}
