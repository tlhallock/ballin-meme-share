
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
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class GotPermission extends Message
{
	public static final int TYPE = 36;
	
	private String rootName;
	private SharingState permission;

	public GotPermission(String rootName, SharingState permission)
	{
		this.rootName = rootName;
		this.permission = permission;
	}
	
	public GotPermission(InputStream input) throws IOException
	{
		super(input);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		rootName = reader.readString();
		permission = reader.readPermission();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(rootName);
		buffer.append(permission);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine remote = connection.getMachine();
		// Don't change all the other settings...
		remote = DbMachines.getMachine(remote.getIdentifier());
		if (remote == null || remote.isLocal())
		{
			return;
		}
		if (rootName.length() == 0)
		{
			if (remote.getSharesWithUs() != null && remote.getSharesWithUs().equals(permission))
			{
				return;
			}
			remote.setTheyShare(permission);
			remote.tryToSave();
			Services.notifications.permissionsChanged(remote);
		}
		else
		{
			RemoteDirectory directory = (RemoteDirectory) DbRoots.getRoot(remote, rootName);
			if (directory == null
					|| (directory.getSharesWithUs() != null && directory.getSharesWithUs().equals(permission)))
			{
				return;
			}
			DbPermissions.setSharingState(remote, directory, permission);
			Services.notifications.permissionsChanged(remote);
		}
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("rootName", rootName);
		generator.write("permission",permission.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsrootName = true;
		boolean needspermission = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsrootName)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs rootName");
				}
				if (needspermission)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs permission");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "rootName":
				needsrootName = false;
				rootName = parser.getString();
				break;
			case "permission":
				needspermission = false;
				permission = SharingState.valueOf(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "GotPermission"; }
	public String getJsonKey() { return getJsonName(); }
	public GotPermission(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
