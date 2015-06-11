package org.cnv.shr.db.h2.bak;

import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class LocalBackup implements Jsonable
{
	private String name;
	private String description;
	@MyParserNullable
	private String tags;
	private long minFSize = -1;
	private long maxFSize = -1;
	private String path;
	@MyParserNullable
	private Long totalFileSize;
	@MyParserNullable
	private Long totalNumFiles;
	@MyParserNullable
	private SharingState defaultSharingState;
	
	LocalBackup(LocalDirectory root)
	{
		name = root.getName();
		this.description = root.getDescription();
		this.tags = root.getTags();
		this.minFSize = root.getMinFileSize();
		this.maxFSize = root.getMaxFileSize();
		this.path = root.getPathElement().getFsPath();
		this.totalFileSize = root.diskSpace();
		this.totalNumFiles = root.numFiles();
	}
	
	public void save(ConnectionWrapper wrapper)
	{
		LocalDirectory local = new LocalDirectory(
				name,
				description,
				tags,
				minFSize,
				maxFSize,
				path,
				defaultSharingState,
				totalFileSize,
				totalNumFiles);
		try
		{
			local.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to restore local: " + name, e);
		}
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("name", name);
		generator.write("description", description);
		if (tags!=null)
		generator.write("tags", tags);
		generator.write("minFSize", minFSize);
		generator.write("maxFSize", maxFSize);
		generator.write("path", path);
		if (totalFileSize!=null)
		generator.write("totalFileSize", totalFileSize);
		if (totalNumFiles!=null)
		generator.write("totalNumFiles", totalNumFiles);
		if (defaultSharingState!=null)
		generator.write("defaultSharingState",defaultSharingState.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsminFSize = true;
		boolean needsmaxFSize = true;
		boolean needsname = true;
		boolean needsdescription = true;
		boolean needspath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsminFSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs minFSize");
				}
				if (needsmaxFSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs maxFSize");
				}
				if (needsname)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
				}
				if (needsdescription)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs description");
				}
				if (needspath)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs path");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "minFSize":
				needsminFSize = false;
				minFSize = Long.parseLong(parser.getString());
				break;
			case "maxFSize":
				needsmaxFSize = false;
				maxFSize = Long.parseLong(parser.getString());
				break;
			case "totalFileSize":
				totalFileSize = Long.parseLong(parser.getString());
				break;
			case "totalNumFiles":
				totalNumFiles = Long.parseLong(parser.getString());
				break;
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "description":
				needsdescription = false;
				description = parser.getString();
				break;
			case "tags":
				tags = parser.getString();
				break;
			case "path":
				needspath = false;
				path = parser.getString();
				break;
			case "defaultSharingState":
				defaultSharingState = SharingState.valueOf(parser.getString());;
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "LocalBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public LocalBackup(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
