
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

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class RootPermissionBackup implements Jsonable
{
	private String machineIdent;
	private String localName;
	private SharingState currentState;
	
	public RootPermissionBackup(LocalDirectory local, Machine machine, SharingState state)
	{
		this.localName = local.getName();
		this.machineIdent = machine.getIdentifier();
		this.currentState = state;
	}
	
	public void save(ConnectionWrapper wrapper)
	{
		LocalDirectory local = DbRoots.getLocalByName(localName);
		Machine machine = DbMachines.getMachine(machineIdent);
		DbPermissions.setSharingState(machine, local, currentState);
	}
	
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("machineIdent", machineIdent);
		generator.write("localName", localName);
		generator.write("currentState",currentState.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsMachineIdent = true;
		boolean needsLocalName = true;
		boolean needsCurrentState = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsMachineIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.RootPermissionBackup\" needs \"machineIdent\"");
				}
				if (needsLocalName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.RootPermissionBackup\" needs \"localName\"");
				}
				if (needsCurrentState)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.RootPermissionBackup\" needs \"currentState\"");
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
				case "localName":
					needsLocalName = false;
					localName = parser.getString();
					break;
				case "currentState":
					needsCurrentState = false;
					currentState = SharingState.valueOf(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "RootPermissionBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public RootPermissionBackup(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
