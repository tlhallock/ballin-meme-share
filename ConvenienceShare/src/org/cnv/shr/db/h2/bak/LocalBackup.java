
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.db.h2.bak;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
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
		if (path.charAt(path.length() - 1) != '/')
		{
			path = path + "/";
		}
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
			return;
		}
		DbPaths.pathLiesIn(DbPaths.getPathElement(path), local);
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
				defaultSharingState = SharingState.valueOf(parser.getString());
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
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
