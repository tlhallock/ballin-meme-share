
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



package org.cnv.shr.msg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class RootListChild implements Jsonable
{
	String name;
	@MyParserNullable
	String tags;
	@MyParserNullable
	String description;
	SharingState isShared;
	
	public RootListChild(ByteReader reader) throws IOException
	{
		parse(reader);
	}
	
	public RootListChild(RootDirectory root)
	{
		name = root.getName();
		tags = root.getTags();
		description = root.getDescription();
		isShared = DbPermissions.getCurrentPermissions(root.getMachine(), (LocalDirectory) root);
	}
	
	void append(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(name);
		buffer.append(tags);
		buffer.append(description);
		buffer.append(isShared);
	}
	
	void parse(ByteReader reader) throws IOException
	{
		name        = reader.readString();
		tags        = reader.readString();
		description = reader.readString();
		isShared    = SharingState.get(reader.readInt());
	}
	
	RemoteDirectory createRoot(Machine machine) throws IOException
	{
		RootDirectory root = DbRoots.getRoot(machine, name);
		if (root != null && !root.isLocal())
		{
			root.setTags(tags);
			root.setDescription(description);
			((RemoteDirectory) root).setSharesWithUs(isShared);
		}
		else
		{
			root = new RemoteDirectory(machine, name, tags, description, isShared);
		}
		root.tryToSave();
		return (RemoteDirectory) root;
	}

	public String getName()
	{
		return name;
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("name", name);
		if (tags!=null)
		generator.write("tags", tags);
		if (description!=null)
		generator.write("description", description);
		generator.write("isShared",isShared.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsName = true;
		boolean needsIsShared = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.RootListChild\" needs \"name\"");
				}
				if (needsIsShared)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.RootListChild\" needs \"isShared\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "name":
					needsName = false;
					name = parser.getString();
					break;
				case "tags":
					tags = parser.getString();
					break;
				case "description":
					description = parser.getString();
					break;
				case "isShared":
					needsIsShared = false;
					isShared = SharingState.valueOf(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "RootListChild"; }
	public String getJsonKey() { return getJsonName(); }
	public RootListChild(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
