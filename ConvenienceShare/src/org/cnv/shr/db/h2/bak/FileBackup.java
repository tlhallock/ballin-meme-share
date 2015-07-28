
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
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.trck.TrackObjectUtils;
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
		this.path = file.getPath().getFullPath();
		this.fileSize = file.getFileSize();
		this.rootName = file.getRootDirectory().getName();
		this.checksum = file.getChecksum();
		this.lastModified = file.getLastUpdated();
		this.tags = file.getTags();
	}
	
	public void save(ConnectionWrapper wrapper)
	{
		LocalDirectory localByName = DbRoots.getLocalByName(rootName);
		PathElement element = DbPaths2.addFilePath(localByName, path);
		LocalFile localFile = new LocalFile(element, tags, fileSize, lastModified, checksum);
		try
		{
			localFile.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to restore file " + path, e);
			return;
		}
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("path", path);
		generator.write("fileSize", fileSize);
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
		boolean needsFileSize = true;
		boolean needsLastModified = true;
		boolean needsPath = true;
		boolean needsRootName = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsFileSize)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.FileBackup\" needs \"fileSize\"");
				}
				if (needsLastModified)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.FileBackup\" needs \"lastModified\"");
				}
				if (needsPath)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.FileBackup\" needs \"path\"");
				}
				if (needsRootName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.FileBackup\" needs \"rootName\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "fileSize":
					needsFileSize = false;
					fileSize = Long.parseLong(parser.getString());
					break;
				case "lastModified":
					needsLastModified = false;
					lastModified = Long.parseLong(parser.getString());
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "path":
					needsPath = false;
					path = parser.getString();
					break;
				case "rootName":
					needsRootName = false;
					rootName = parser.getString();
					break;
				case "checksum":
					checksum = parser.getString();
					break;
				case "tags":
					tags = parser.getString();
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "FileBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public FileBackup(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
