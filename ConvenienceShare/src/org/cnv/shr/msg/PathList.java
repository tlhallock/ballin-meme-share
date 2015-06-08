package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.json.JsonStringList;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class PathList extends Message
{
	public static int TYPE = 19;
	
	private String name;
	private String currentPath;
	private JsonStringList subDirs = new JsonStringList();
	private JsonList<PathListChild> children = new JsonList<>(PathListChild.getJsonName());

	public PathList(final InputStream input) throws IOException
	{
		super(input);
	}	

	public PathList(final LocalDirectory localByName, final PathElement pathElement)
	{
		name = localByName.getName();
		currentPath = pathElement.getFullPath();
		for (final PathElement element : pathElement.list(localByName))
		{
			if (element.getId() == 0 || element.getFullPath().equals("/"))
			{
				continue;
			}
			final SharedFile local = DbFiles.getFile(localByName, element);
			if (local == null)
			{
				subDirs.add(element.getUnbrokenName());
			}
			else
			{
				children.add(new PathListChild(this, local));
			}
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();
		
		System.out.println("[machine=" + name + "   currentpath=" + currentPath + "]:");
		builder.append("Subdirectories:\n");
		for (final String subdir : subDirs)
		{
			builder.append('\t').append(subdir).append('\n');
		}
		builder.append("Files:\n");
		for (final PathListChild c : children)
		{
			builder.append('\t').append(c.name).append('\n');
		}
		
		return builder.toString();
	}

	@Override
	protected void parse(final ByteReader reader) throws IOException
	{
		name = reader.readString();
		currentPath = reader.readString();
		final int numDirs = reader.readInt();
		for (int i = 0; i < numDirs; i++)
		{
			subDirs.add(reader.readString());
		}
		final int numFiles = reader.readInt();
		for (int i = 0; i < numFiles; i++)
		{
			PathListChild e = new PathListChild(reader);
			e.setParent(this);
			children.add(e);
		}
	}

	@Override
	protected void print(Communication connection, final AbstractByteWriter buffer) throws IOException
	{
		buffer.append(name);
		buffer.append(currentPath);
		buffer.append(subDirs.size());
		for (final String sub : subDirs)
		{
			buffer.append(sub);
		}
		buffer.append(children.size());
		for (final PathListChild c : children)
		{
			c.write(buffer);
		}
	}

	@Override
	public void perform(final Communication connection) throws Exception
	{
		getRoot(connection.getMachine());
		final RemoteSynchronizerQueue sync = Services.syncs.getSynchronizer(connection, getRoot());
		if (sync == null)
		{
			LogWrapper.getLogger().info("Lost synchronizer?");
			return;
		}
		sync.receiveList(this);
	}

	public String getCurrentPath()
	{
		return getPath().getFullPath();
	}
	
	@MyParserIgnore
	RemoteDirectory rootCache;
	RemoteDirectory getRoot(final Machine machine)
	{
		if (rootCache != null)
		{
			return rootCache;
		}
		return rootCache = (RemoteDirectory) DbRoots.getRoot(machine, name);
	}

	RemoteDirectory getRoot()
	{
		return rootCache;
	}

	@MyParserIgnore
	PathElement elemCache;
	PathElement getPath()
	{
		if (elemCache != null)
		{
			return elemCache;
		}
		return elemCache = DbPaths.getPathElement(currentPath);
	}

	public LinkedList<PathListChild> getChildren()
	{
		return children;
	}

	public LinkedList<String> getSubDirs()
	{
		return subDirs;
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}
	

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (name!=null)
		generator.write("name", name);
		if (currentPath!=null)
		generator.write("currentPath", currentPath);
		if (subDirs!=null){
			generator.writeStartArray("subDirs");
			subDirs.generate(generator);
		}
		if (children!=null){
			generator.writeStartArray("children");
			children.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsname = true;
		boolean needscurrentPath = true;
		boolean needssubDirs = true;
		boolean needschildren = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needscurrentPath)
				{
					throw new RuntimeException("Message needs currentPath");
				}
				if (needssubDirs)
				{
					throw new RuntimeException("Message needs subDirs");
				}
				if (needschildren)
				{
					throw new RuntimeException("Message needs children");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "currentPath":
				needscurrentPath = false;
				currentPath = parser.getString();
				break;
			}
			break;
		case START_ARRAY:
			if (key==null) break;
			switch(key) {
			case "subDirs":
				needssubDirs = false;
				subDirs.parse(parser);
				break;
			case "children":
				needschildren = false;
				children.parse(parser);
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "PathList"; }
	public PathList(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
