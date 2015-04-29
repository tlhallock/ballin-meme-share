package org.cnv.shr.msg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.util.ByteListBuffer;

public class FileList extends Message
{
	private LinkedList<File> files;

	public FileList(LocalDirectory local)
	{
		
	}

	@Override
	public void perform()
	{
		// Machine m = Remotes.getInstance().getMachine(getMachine());
		// set remote directories

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
