package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.AbstractByteWriter;

public class DoneMessage extends Message
{
	public static int TYPE = 1;
	
	public DoneMessage() {}
	public DoneMessage(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}

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
}
