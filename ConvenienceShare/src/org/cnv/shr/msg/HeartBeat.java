package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class HeartBeat extends Message
{

	@Override
	public void perform()
	{
		Services.remotes.isAlive(getMachine());
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		// TODO Auto-generated method stub
		
	}

}
