
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
import java.util.HashSet;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class RootList extends Message
{
	private JsonList<RootListChild> sharedDirectories = new JsonList<>(
			new JsonList.Allocator<RootListChild>()
			{
				public RootListChild create(JsonParser parser)
				{
					return new RootListChild(parser);
				}
			});

	public RootList()
	{
		try (DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();)
		{
			while (listLocals.hasNext())
			{
				add(listLocals.next());
			}
		}
	}
	private void add(RootDirectory root)
	{
		sharedDirectories.add(new RootListChild(root));
	}

	@Override
	public void perform(Communication connection)
	{
		HashSet<String> accountedFor = new HashSet<>();
		Machine machine = connection.getMachine();
		
		for (RootListChild rootC : sharedDirectories)
		{
			try
			{
				RootDirectory root = rootC.createRoot(connection.getMachine());
				accountedFor.add(root.getName());
			}
			catch (IOException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to create root " + rootC.getName(), ex);
			}
		}

//		List<RootDirectory> toDelete = new LinkedList<>();
		try (DbIterator<RootDirectory> list = DbRoots.list(machine);)
		{
			while (list.hasNext())
			{
				final RootDirectory next = list.next();
				if (accountedFor.contains(next.getName()))
				{
					continue;
				}
//				toDelete.add(next);
				DbRoots.deleteRoot(next, true);
			}
		}
		
//		for (RootDirectory root : toDelete)
//		{
//			DbRoots.deleteRoot(root, true);
//			// should be deleted...
//			Services.notifications.remoteDirectoryChanged((RemoteDirectory) root);
//		}

		// TODO: should only happen if there was a change...
		Services.notifications.remoteChanged(machine);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Root directories are: ");
		for (RootListChild directory : sharedDirectories)
		{
			builder.append(directory.getName()).append(':');
		}
		
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		{
			generator.writeStartArray("sharedDirectories");
			sharedDirectories.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsSharedDirectories = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsSharedDirectories)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.RootList\" needs \"sharedDirectories\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_ARRAY:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("sharedDirectories")) {
					needsSharedDirectories = false;
					sharedDirectories.parse(parser);
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "RootList"; }
	public String getJsonKey() { return getJsonName(); }
	public RootList(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
