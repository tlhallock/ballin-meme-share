package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ListRoots extends Message
{
	public ListRoots() {}
	
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
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(AbstractByteWriter buffer) {}
	
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
