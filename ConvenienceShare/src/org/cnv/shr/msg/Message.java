package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.OutputByteWriter;

public abstract class Message
{
	private static final int VERSION = 1;
	
	protected Message() {}
	
	// This constructor is no longer needed.
	protected Message(InputStream stream) throws IOException {}
	
	public boolean requiresAthentication()
	{
		return true;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Please implement toString() in class " + getClass().getName());
		
		return builder.toString();
	}
	
	public final void write(OutputByteWriter output) throws IOException
	{
		output.append(getType());
		print(output);
	}
	
	public void read(ByteReader reader) throws IOException
	{
		parse(reader);
	}

	protected abstract int  getType();
	protected abstract void parse(ByteReader reader) throws IOException;
	protected abstract void print(AbstractByteWriter buffer) throws IOException;
	public    abstract void perform(Communication connection) throws Exception;
}
