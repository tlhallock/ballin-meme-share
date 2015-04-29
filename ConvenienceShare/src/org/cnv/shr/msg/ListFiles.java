package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.util.ByteListBuffer;

public class ListFiles extends Message
{

	@Override
	public void perform()
	{
		for (LocalDirectory local : Services.locals.listLocals())
		{
//			try
//			{
//				new FileList(local).send(getMachine());
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
		}
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
	
	public static int TYPE = 1;
	protected int getType()
	{
		return TYPE;
	}
}
