package org.cnv.shr.msg;

import java.io.IOException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Jsonable;

public class PathListChild implements Jsonable
{
	String name;
	private long size;
	private String checksum;
	private String tags;
	private long lastModified;

	@MyParserIgnore
	PathList pl;
	
	PathListChild(PathList pathList, final SharedFile l)
	{
		this.name = l.getPath().getUnbrokenName();
		this.size = l.getFileSize();
		this.checksum = l.getChecksum() == null ? "" : l.getChecksum();
		this.tags = l.getTags();
		this.lastModified = l.getLastUpdated();
		this.pl = pathList;
	}
	
	void setParent(PathList pl)
	{
		this.pl = pl;
	}
	
	PathListChild (final ByteReader bytes) throws IOException
	{
		name = bytes.readString();
		size = bytes.readLong();
		checksum = bytes.readString();
		tags = bytes.readString();
		lastModified = bytes.readLong();
	}
	
	public void write(final AbstractByteWriter buffer) throws IOException
	{
		buffer.append(name);
		buffer.append(size);
		buffer.append(checksum);
		buffer.append(tags == null ? "" : tags);
		buffer.append(lastModified);
	}
	
	public RemoteFile create() 
	{
		final PathElement pathElement = DbPaths.getPathElement(pl.getPath(), name);
		return new RemoteFile(pl.getRoot(), pathElement,
				size, checksum, tags, lastModified);
	}


	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (name!=null)
		generator.write("name", name);
		generator.write("size", size);
		if (checksum!=null)
		generator.write("checksum", checksum);
		if (tags!=null)
		generator.write("tags", tags);
		generator.write("lastModified", lastModified);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsname = true;
		boolean needschecksum = true;
		boolean needstags = true;
		boolean needssize = true;
		boolean needslastModified = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needschecksum)
				{
					throw new RuntimeException("Message needs checksum");
				}
				if (needstags)
				{
					throw new RuntimeException("Message needs tags");
				}
				if (needssize)
				{
					throw new RuntimeException("Message needs size");
				}
				if (needslastModified)
				{
					throw new RuntimeException("Message needs lastModified");
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
			case "checksum":
				needschecksum = false;
				checksum = parser.getString();
				break;
			case "tags":
				needstags = false;
				tags = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "size":
				needssize = false;
				size = Long.parseLong(parser.getString());
				break;
			case "lastModified":
				needslastModified = false;
				lastModified = Long.parseLong(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "PathListChild"; }
	public PathListChild(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
