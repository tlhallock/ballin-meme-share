
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
import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class ChunkResponse extends DownloadMessage
{
	private Chunk chunk;
	
	private boolean isCompressed;
	
	public static int TYPE = 14;
	
	public ChunkResponse(FileEntry descriptor, Chunk c, boolean compressed)
	{
		super(descriptor);
		chunk = c;
		this.isCompressed = compressed;
	}
	
	public ChunkResponse(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		chunk = new Chunk(reader);
	}
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		chunk.write(buffer);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor(), connection);
		if (downloadInstance == null)
		{
			// download not found...
			// TODO: what should we actually do here.
			LogWrapper.getLogger().info("Could not find download for " + getDescriptor() + "!!");
			connection.finish();
			return;
		}
		downloadInstance.download(chunk, connection, isCompressed);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Giving you chunk " + chunk);
		
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		chunk.generate(generator, "chunk");
		generator.write("isCompressed", isCompressed);
		descriptor.generate(generator, "descriptor");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsIsCompressed = true;
		boolean needsChunk = true;
		boolean needsDescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsIsCompressed)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs isCompressed");
				}
				if (needsChunk)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs chunk");
				}
				if (needsDescriptor)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs descriptor");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_FALSE:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("isCompressed")) {
					needsIsCompressed = false;
					isCompressed = false;
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_TRUE:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("isCompressed")) {
					needsIsCompressed = false;
					isCompressed = true;
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case START_OBJECT:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "chunk":
					needsChunk = false;
					chunk = new Chunk(parser);
					break;
				case "descriptor":
					needsDescriptor = false;
					descriptor = new FileEntry(parser);
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ChunkResponse"; }
	public String getJsonKey() { return getJsonName(); }
	public ChunkResponse(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
