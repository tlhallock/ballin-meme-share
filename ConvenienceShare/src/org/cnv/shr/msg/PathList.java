
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
import java.util.LinkedList;
import java.util.Set;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.json.JsonSet;
import org.cnv.shr.json.JsonStringSet;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class PathList extends Message
{
	public static int TYPE = 19;

	private boolean isTheEnd = false;
	private String name;
	private String currentPath;
	private JsonStringSet subDirs = new JsonStringSet();
	private JsonSet<PathListChild> children = new JsonSet<>(
			new JsonList.Allocator<PathListChild>()
			{
				public PathListChild create(JsonParser parser)
				{
					return new PathListChild(parser);
				}});

	public PathList(final LocalDirectory localByName, final PathElement pathElement)
	{
		name = localByName.getName();
		currentPath = pathElement.getFullPath();
	}
	
	static void listPaths(final LocalDirectory localByName, final PathElement pathElement, Communication communication) throws IOException, PermissionException
	{
		checkPermissionsVisible(communication, communication.getMachine(), localByName, "List a path");

		System.out.println("Listing " + localByName.getName() + ":" + pathElement.getFullPath());
		
		int count = 0;
		PathList currentMessage = new PathList(localByName, pathElement);
		for (final PathElement element : pathElement.list(localByName))
		{
			if (element.getId() == 0 || element.getFullPath().equals("/"))
			{
				continue;
			}
			final SharedFile local = DbFiles.getFile(localByName, element);
			
			if (count++ > 50)
			{
				// TODO: compress this
				communication.send(currentMessage);
				currentMessage.subDirs.clear();
				currentMessage.children.clear();
			}
			
			if (local == null)
			{
				currentMessage.subDirs.add(element.getUnbrokenName());
			}
			else
			{
				currentMessage.children.add(new PathListChild(currentMessage, local));
			}
		}
		// TODO: and this
		currentMessage.isTheEnd = true;
		communication.send(currentMessage);
	}
	
	public boolean listIsComplete()
	{
		return isTheEnd;
	}
	
	public void merge(PathList other)
	{
		if (!name.equals(other.name))
		{
			throw new RuntimeException("Cannot merge: names do not match.");
		}
		if (!currentPath.equals(other.currentPath))
		{
			throw new RuntimeException("Cannot merge: currentPath does not match.");
		}
		
		subDirs.addAll(other.subDirs);
		children.addAll(other.children);
	}
	
	@Override
	public String toString()
	{
//		final StringBuilder builder = new StringBuilder();
//		
//		System.out.println("[machine=" + name + "   currentpath=" + currentPath + "]:");
//		builder.append("Subdirectories:\n");
//		for (final String subdir : subDirs)
//		{
//			builder.append('\t').append(subdir).append('\n');
//		}
//		builder.append("Files:\n");
//		for (final PathListChild c : children)
//		{
//			builder.append('\t').append(c.name).append('\n');
//		}
//		
//		return builder.toString();

		final StringBuilder builder = new StringBuilder();
		System.out.println("[machine=" + name + "   currentpath=" + currentPath + "]:");
		builder.append("\tSubdirectories: " + subDirs.size());
		builder.append("\tFiles: " + children.size());
		return builder.toString();
	
	}

	@Override
	protected void parse(final ByteReader reader) throws IOException
	{
		name = reader.readString();
		currentPath = reader.readString();
		final int numDirs = reader.readInt();
		for (int i = 0; i < numDirs; i++)
		{
			subDirs.add(reader.readString());
		}
		final int numFiles = reader.readInt();
		for (int i = 0; i < numFiles; i++)
		{
			PathListChild e = new PathListChild(reader);
			e.setParent(this);
			children.add(e);
		}
	}

	@Override
	protected void print(Communication connection, final AbstractByteWriter buffer) throws IOException
	{
		buffer.append(name);
		buffer.append(currentPath);
		buffer.append(subDirs.size());
		for (final String sub : subDirs)
		{
			buffer.append(sub);
		}
		buffer.append(children.size());
		for (final PathListChild c : children)
		{
			c.write(buffer);
		}
	}

	@Override
	public void perform(final Communication connection) throws Exception
	{
		for (PathListChild child : children)
		{
			child.setParent(this);
		}
		getRoot(connection.getMachine());
		final RemoteSynchronizerQueue sync = Services.syncs.getSynchronizer(connection, getRoot());
		if (sync == null)
		{
			LogWrapper.getLogger().info("Lost synchronizer?");
			return;
		}
		sync.receiveList(this);
	}

	public String getCurrentPath()
	{
		return getPath().getFullPath();
	}
	
	@MyParserIgnore
	RemoteDirectory rootCache;
	RemoteDirectory getRoot(final Machine machine)
	{
		if (rootCache != null)
		{
			return rootCache;
		}
		return rootCache = (RemoteDirectory) DbRoots.getRoot(machine, name);
	}

	RemoteDirectory getRoot()
	{
		return rootCache;
	}

	@MyParserIgnore
	PathElement elemCache;
	PathElement getPath()
	{
		if (elemCache != null)
		{
			return elemCache;
		}
		return elemCache = DbPaths.getPathElement(currentPath);
	}

	public LinkedList<PathListChild> getChildren()
	{
		return children;
	}

	public Set<String> getSubDirs()
	{
		return subDirs;
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}
	

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("isTheEnd", isTheEnd);
		generator.write("name", name);
		generator.write("currentPath", currentPath);
		{
			generator.writeStartArray("subDirs");
			subDirs.generate(generator);
		}
		{
			generator.writeStartArray("children");
			children.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsSubDirs = true;
		boolean needsChildren = true;
		boolean needsIsTheEnd = true;
		boolean needsName = true;
		boolean needsCurrentPath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsSubDirs)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs subDirs");
				}
				if (needsChildren)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs children");
				}
				if (needsIsTheEnd)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs isTheEnd");
				}
				if (needsName)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
				}
				if (needsCurrentPath)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs currentPath");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_ARRAY:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "subDirs":
					needsSubDirs = false;
					subDirs.parse(parser);
					break;
				case "children":
					needsChildren = false;
					children.parse(parser);
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_FALSE:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("isTheEnd")) {
					needsIsTheEnd = false;
					isTheEnd = false;
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_TRUE:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("isTheEnd")) {
					needsIsTheEnd = false;
					isTheEnd = true;
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "name":
					needsName = false;
					name = parser.getString();
					break;
				case "currentPath":
					needsCurrentPath = false;
					currentPath = parser.getString();
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "PathList"; }
	public String getJsonKey() { return getJsonName(); }
	public PathList(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
