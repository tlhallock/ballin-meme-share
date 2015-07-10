
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
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbRoots.IgnorePatterns;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.json.JsonStringSet;
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
	private JsonStringSet ignores = new JsonStringSet();
	
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
		IgnorePatterns dbIgnores = DbRoots.getIgnores(root);
		for (String str : dbIgnores.getPatterns())
		{
			ignores.add(str);
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
		DbPaths.pathLiesIn(DbPaths.getPathElement(path, false), local);
		
		DbRoots.setIgnores(local, ignores.toArray(DUMMY));
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
		{
			generator.writeStartArray("ignores");
			ignores.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsIgnores = true;
		boolean needsMinFSize = true;
		boolean needsMaxFSize = true;
		boolean needsName = true;
		boolean needsDescription = true;
		boolean needsPath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsIgnores)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ignores");
				}
				if (needsMinFSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs minFSize");
				}
				if (needsMaxFSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs maxFSize");
				}
				if (needsName)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
				}
				if (needsDescription)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs description");
				}
				if (needsPath)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs path");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_ARRAY:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("ignores")) {
					needsIgnores = false;
					ignores.parse(parser);
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_NUMBER:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "minFSize":
					needsMinFSize = false;
					minFSize = Long.parseLong(parser.getString());
					break;
				case "maxFSize":
					needsMaxFSize = false;
					maxFSize = Long.parseLong(parser.getString());
					break;
				case "totalFileSize":
					totalFileSize = Long.parseLong(parser.getString());
					break;
				case "totalNumFiles":
					totalNumFiles = Long.parseLong(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "name":
					needsName = false;
					name = parser.getString();
					break;
				case "description":
					needsDescription = false;
					description = parser.getString();
					break;
				case "tags":
					tags = parser.getString();
					break;
				case "path":
					needsPath = false;
					path = parser.getString();
					break;
				case "defaultSharingState":
					defaultSharingState = SharingState.valueOf(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
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
	
	
	private static final String[] DUMMY = new String[0];
}
