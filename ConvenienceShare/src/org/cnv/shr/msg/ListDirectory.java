package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class ListDirectory extends Message
{
	String rootName;
	String path;
	
	public ListDirectory(RemoteDirectory remote, PathElement path)
	{
		rootName = remote.getName();
		this.path = path.getFullPath();
	}
	
	public ListDirectory(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}
	

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		rootName = ByteReader.readString(bytes);
		path = ByteReader.readString(bytes);
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(rootName);
		buffer.append(path);
	}
	
	public static int TYPE = 10;

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LocalDirectory localByName = DbRoots.getLocalByName(rootName);
		PathElement pathElement = DbPaths.getPathElement(path);
		connection.send(new DirectoryList(localByName, pathElement));
	}
	
}
