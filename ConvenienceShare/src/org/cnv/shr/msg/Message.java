package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.OutputByteWriter;

public abstract class Message
{
	private static final int VERSION = 1;
	
	protected Message() {}
	
	// This constructor is no longer needed.
	protected Message(InputStream stream) throws IOException
	{
//		parse(stream);
	}

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
	
	public final void write(OutputStream output) throws IOException
	{
		OutputByteWriter writer = new OutputByteWriter(output);
		writer.append(getType());
		write(writer);
		output.flush();
	}

	protected abstract int  getType();
	public abstract void parse(InputStream bytes) throws IOException;
	protected abstract void write(AbstractByteWriter buffer) throws IOException;
	public    abstract void perform(Communication connection) throws Exception;
}
