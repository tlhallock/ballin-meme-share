package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

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
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
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
		connection.send(new DoneResponse());
		connection.setDone();
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
