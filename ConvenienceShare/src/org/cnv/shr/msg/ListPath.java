package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.util.AbstractByteWriter;
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
	
	public ListPath(InputStream stream) throws IOException
	{
		super(stream);
	}
	

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		rootName = reader.readString();
		path = reader.readString();
	}

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
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
		PathList msg = new PathList(localByName, pathElement);

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
