
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



package org.cnv.shr.msg.key;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class PermissionFailure extends Message
{
	public static int TYPE = 35;
	
	private String rootName;
	private SharingState currentPermission;
	private String action;
	
	public PermissionFailure(String root, SharingState current, String action)
	{
		this.rootName = root;
		this.currentPermission = current;
		if (currentPermission == null)
		{
			currentPermission = SharingState.DO_NOT_SHARE;
		}
		this.action = action;
	}
	
	public PermissionFailure(InputStream input) throws IOException
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
		currentPermission = SharingState.get(reader.readInt());
		action = reader.readString();
		
		if (rootName.length() == 0)
		{
			rootName = null;
		}
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		if (rootName == null)
		{
			buffer.append("");
		}
		else
		{
			buffer.append(rootName);
		}
		buffer.append(currentPermission.getDbValue());
		buffer.append(action);
	}

	@Override
	public void perform(Communication connection)
	{
		PermissionFailureEvent event = new PermissionFailureEvent();
		event.machine = connection.getMachine();
		event.rootName = rootName;
		event.currentPermissions = currentPermission;
		event.action = action;
		
		try
		{
			updateDb(event.machine);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to update permissions for machine " + event.machine, e);
		}
		
		Services.notifications.permissionFailure(event);
		connection.finish();
	}
	
	private void updateDb(Machine machine) throws SQLException
	{
		if (rootName != null)
		{
			RootDirectory root = DbRoots.getRoot(machine, rootName);
			if (!(root instanceof RemoteDirectory))
			{
				return;
			}
			((RemoteDirectory) root).setSharesWithUs(currentPermission);
		}
		else
		{
			machine.setTheyShare(currentPermission);
			machine.tryToSave();
		}
	}
	
	
	public static class PermissionFailureEvent
	{
		private Machine machine;
		private String rootName;
		private SharingState currentPermissions;
		private String action;
		
		private PermissionFailureEvent() {}
		
		public void show(JFrame frame)
		{
			JOptionPane.showMessageDialog(frame, 
					"Permission was denied while trying action: " + action + ".\n" +
					"Current permissions are " + currentPermissions + ".\n" +
				    "Remote machine: " + machine.getName() + ".\n" +
					(rootName == null ? "" : "Root: " + rootName + ".\n"),
					"Permission failure for " + machine.getName() + ".\n" + 
					"You may be able to hit request permissions...",
					JOptionPane.ERROR_MESSAGE);
		}

		public SharingState getCurrentSharingState()
		{
			return currentPermissions;
		}

		public String getRootName()
		{
			return rootName;
		}
		
		public String getMachineName()
		{
			return machine.getName();
		}

		public String getMachineIdent()
		{
			return machine.getIdentifier();
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
		generator.write("currentPermission",currentPermission.name());
		generator.write("action", action);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsRootName = true;
		boolean needsCurrentPermission = true;
		boolean needsAction = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsRootName)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs rootName");
				}
				if (needsCurrentPermission)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs currentPermission");
				}
				if (needsAction)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs action");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "rootName":
					needsRootName = false;
					rootName = parser.getString();
					break;
				case "currentPermission":
					needsCurrentPermission = false;
					currentPermission = SharingState.valueOf(parser.getString());
					break;
				case "action":
					needsAction = false;
					action = parser.getString();
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "PermissionFailure"; }
	public String getJsonKey() { return getJsonName(); }
	public PermissionFailure(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
