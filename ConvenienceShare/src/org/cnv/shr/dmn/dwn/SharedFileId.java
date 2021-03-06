
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



package org.cnv.shr.dmn.dwn;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class SharedFileId implements Jsonable
{
	// Either
	private String machineIdent;
	private String rootName;
	private String path;
	
	public SharedFileId(SharedFile file)
	{
		this.machineIdent = file.getRootDirectory().getMachine().getIdentifier();
		this.rootName = file.getRootDirectory().getName();
		this.path = file.getPath().getFullPath();
	}

	public SharedFileId(String readString, String readString2, String readString3)
	{
		this.machineIdent = readString;
		this.rootName = readString2;
		this.path = readString3;
	}

	public String getMachineIdent()
	{
		return machineIdent;
	}

	public String getRootName()
	{
		return rootName;
	}

	public String getPath()
	{
		return path;
	}

	@Override
	public int hashCode()
	{
		return (machineIdent + ":" + rootName + ":" + path).hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof SharedFileId))
		{
			return false;
		}
		SharedFileId o = (SharedFileId) other;
		return machineIdent.equals(o.machineIdent)
				&& rootName.equals(o.rootName)
				&& path.equals(path);
	}
	
	public LocalFile getLocal()
	{
		LocalDirectory local = DbRoots.getLocalByName(rootName);
		PathElement pathElement = DbPaths2.findFilePath(local, path);
		return DbFiles.getLocalFile(pathElement);
	}
	
	public RemoteFile getRemote()
	{
		Machine machine = DbMachines.getMachine(machineIdent);
		RootDirectory root = DbRoots.getRoot(machine, rootName);
		PathElement pathElement = DbPaths2.findFilePath(root, path);
		return DbFiles.getRemoteFile(pathElement);
	}
	
	@Override
	public String toString()
	{
		return machineIdent + " : " + rootName + " : " + path;
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("machineIdent", machineIdent);
		generator.write("rootName", rootName);
		generator.write("path", path);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsMachineIdent = true;
		boolean needsRootName = true;
		boolean needsPath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsMachineIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.dmn.dwn.SharedFileId\" needs \"machineIdent\"");
				}
				if (needsRootName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.dmn.dwn.SharedFileId\" needs \"rootName\"");
				}
				if (needsPath)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.dmn.dwn.SharedFileId\" needs \"path\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "machineIdent":
					needsMachineIdent = false;
					machineIdent = parser.getString();
					break;
				case "rootName":
					needsRootName = false;
					rootName = parser.getString();
					break;
				case "path":
					needsPath = false;
					path = parser.getString();
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "SharedFileId"; }
	public String getJsonKey() { return getJsonName(); }
	public SharedFileId(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
