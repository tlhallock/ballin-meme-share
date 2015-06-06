package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

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
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
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
		checkPermissionsVisible(connection, connection.getMachine(), localByName, "List a path");
		PathElement pathElement = DbPaths.getPathElement(path);
		PathList msg = new PathList(localByName, pathElement);

		System.out.println("Listing " + rootName + ":" + path);
		System.out.println("Msg: " + msg);
		
		connection.send(msg);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("What files are under ").append(rootName).append(":").append(path);
		return builder.toString();
	}
	
	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (rootName!=null)
		generator.write("rootName", rootName);
		if (path!=null)
		generator.write("path", path);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsrootName = true;
		boolean needspath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsrootName)
				{
					throw new RuntimeException("Message needs rootName");
				}
				if (needspath)
				{
					throw new RuntimeException("Message needs path");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "rootName":
				needsrootName = false;
				rootName = parser.getString();
				break;
			case "path":
				needspath = false;
				path = parser.getString();
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "ListPath"; }
	public ListPath(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
