
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



package org.cnv.shr.msg.dwn;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class ChunkList extends DownloadMessage
{
	private JsonList<Chunk> chunks = new JsonList<>(
			new JsonList.Allocator<Chunk>()
			{
				public Chunk create(JsonParser parser)
				{
					return new Chunk(parser);
				}});
	
	public ChunkList(List<Chunk> chunks2, FileEntry descriptor)
	{
		super(descriptor);
		chunks.addAll(chunks2);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor(), connection);
		if (downloadInstance == null)
		{
			LogWrapper.getLogger().info("Found chunks, but download no longer present.");
			connection.finish();
			return;
		}
		downloadInstance.foundChunks(connection.getMachine(), chunks);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Listing chunks: [size=").append(chunks.size()).append("]");
		for (Chunk c : chunks)
		{
			builder.append(c);
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
			generator.writeStartArray("chunks");
			chunks.generate(generator);
		}
		descriptor.generate(generator, "descriptor");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsDescriptor = true;
		boolean needsChunks = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsDescriptor)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.dwn.DownloadMessage\" needs \"descriptor\"");
				}
				if (needsChunks)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.dwn.ChunkList\" needs \"chunks\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_OBJECT:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("descriptor")) {
					needsDescriptor = false;
					descriptor = new FileEntry(parser);
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			case START_ARRAY:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("chunks")) {
					needsChunks = false;
					chunks.parse(parser);
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ChunkList"; }
	public String getJsonKey() { return getJsonName(); }
	public ChunkList(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
