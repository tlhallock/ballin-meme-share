
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class RootList extends Message
{
	public static int TYPE = 3;
	
	private JsonList<RootListChild> sharedDirectories = new JsonList<>(RootListChild.getJsonName());

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
	
	public RootList(InputStream i) throws IOException
	{
		super(i);
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
		
		boolean changed = true;
		for (RootListChild rootC : sharedDirectories)
		{
			RootDirectory root = rootC.getRoot(connection.getMachine());
			accountedFor.add(root.getName());
			root.setMachine(machine);
			root.tryToSave();
			changed = true;
		}

		List<RootDirectory> toDelete = new LinkedList<>();
		try (DbIterator<RootDirectory> list = DbRoots.list(machine);)
		{
			while (list.hasNext())
			{
				final RootDirectory next = list.next();
				if (accountedFor.contains(next.getName()))
				{
					continue;
				}
				toDelete.add(next);
				DbRoots.deleteRoot(next);
				changed = true;
			}
		}
		
		for (RootDirectory root : toDelete)
		{
			DbRoots.deleteRoot(root);
			// should be deleted...
			Services.notifications.remoteDirectoryChanged((RemoteDirectory) root);
		}
		
		if (changed)
		{
			Services.notifications.remoteChanged(machine);
		}
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		int numFolders = reader.readInt();
		for (int i = 0; i < numFolders; i++)
		{
			sharedDirectories.add(new RootListChild(reader));
		}
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(Services.localMachine.getIdentifier());
		buffer.append(sharedDirectories.size());
		for (RootListChild dir : sharedDirectories)
		{
			dir.append(buffer);
		}
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
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
		boolean needssharedDirectories = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needssharedDirectories)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs sharedDirectories");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_ARRAY:
			if (key==null) break;
			if (key.equals("sharedDirectories")) {
				needssharedDirectories = false;
				sharedDirectories.parse(parser);
			}
			break;
			default: break;
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
