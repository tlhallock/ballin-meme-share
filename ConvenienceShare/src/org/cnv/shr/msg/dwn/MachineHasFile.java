package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class MachineHasFile extends Message
{
	private boolean hasFile;
	private String path;
	
	public static int TYPE = 17;

	public MachineHasFile(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		
	}
	
	@Override
	protected void write(ByteListBuffer buffer)
	{
		
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I got it! ").append(hasFile).append(":").append(path);
		
		return builder.toString();
	}
}
