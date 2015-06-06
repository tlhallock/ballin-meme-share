package org.cnv.shr.db.h2.bak;

import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class FileBackup implements Jsonable
{
	private String path;
	private long fileSize;
	private String rootName;
	@MyParserNullable
	private String checksum;
	private long lastModified;
	
	@MyParserNullable
	private String tags;
	
	public FileBackup(LocalFile file)
	{
		this.path = file.getFullPath();
		this.fileSize = file.getFileSize();
		this.rootName = file.getRootDirectory().getName();
		this.checksum = file.getChecksum();
		this.lastModified = file.getLastUpdated();
		this.tags = file.getTags();
	}
	
	public void save(ConnectionWrapper wrapper)
	{
		LocalDirectory localByName = DbRoots.getLocalByName(rootName);
		PathElement element = DbPaths.getPathElement(path);
		LocalFile localFile = new LocalFile(localByName, element, tags, fileSize, lastModified, checksum);
		try
		{
			localFile.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to restore file " + path, e);
		}
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (path!=null)
		generator.write("path", path);
		generator.write("fileSize", fileSize);
		if (rootName!=null)
		generator.write("rootName", rootName);
		if (checksum!=null)
		generator.write("checksum", checksum);
		generator.write("lastModified", lastModified);
		if (tags!=null)
		generator.write("tags", tags);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needspath = true;
		boolean needsrootName = true;
		boolean needsfileSize = true;
		boolean needslastModified = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needspath)
				{
					throw new RuntimeException("Message needs path");
				}
				if (needsrootName)
				{
					throw new RuntimeException("Message needs rootName");
				}
				if (needsfileSize)
				{
					throw new RuntimeException("Message needs fileSize");
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
			case "path":
				needspath = false;
				path = parser.getString();
				break;
			case "rootName":
				needsrootName = false;
				rootName = parser.getString();
				break;
			case "checksum":
				checksum = parser.getString();
				break;
			case "tags":
				tags = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "fileSize":
				needsfileSize = false;
				fileSize = Long.parseLong(parser.getString());
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
	public String getJsonName() { return "FileBackup"; }
	public FileBackup(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
