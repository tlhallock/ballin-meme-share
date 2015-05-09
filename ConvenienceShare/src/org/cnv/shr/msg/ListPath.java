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

public class ListPath extends Message
{
	private String rootName;
	private String path;
	
	public ListPath(RemoteDirectory remote, PathElement path)
	{
		rootName = remote.getName();
		this.path = path.getFullPath();
	}
	
	public ListPath(InetAddress address, InputStream stream) throws IOException
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
		DirectoryList msg = new DirectoryList(localByName, pathElement);

		System.out.println("Listing " + rootName + ":" + path);
		System.out.println("Msg: " + msg);
		
		connection.send(msg);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("What files are under ").append(rootName).append(":").append(path);
		return builder.toString();
	}
}
