package org.cnv.shr.msg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.LinkedList;

import org.cnv.shr.dmn.Connection;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.util.ByteListBuffer;

public class FileList extends Message
{
	private LinkedList<File> files;

	public FileList(LocalDirectory local)
	{
		
	}
	public FileList(InetAddress a, InputStream i) throws IOException
	{
		super(a, i);
	}

	@Override
	public void perform(Connection connection)
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

	
	public static int TYPE = 3;
	protected int getType()
	{
		return TYPE;
	}
}
