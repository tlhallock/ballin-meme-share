package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.AbstractByteWriter;

public class ListRoots extends Message
{
	public ListRoots() {
		}
	public ListRoots(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	public void perform(Communication connection)
	{
		connection.send(new RootList());
	}

	@Override
	protected void parse(InputStream bytes) throws IOException {}

	@Override
	protected void write(AbstractByteWriter buffer) {}
	
	public static int TYPE = 6;
	protected int getType()
	{
		return TYPE;
	}

	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("List your root directories.");
		return builder.toString();
	}
}
