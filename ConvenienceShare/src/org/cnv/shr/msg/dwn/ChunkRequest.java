
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
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.msg.Wait;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class ChunkRequest extends DownloadMessage
{
	private Chunk chunk;
	private boolean pleaseCompress;

	public static int TYPE = 16;
	public ChunkRequest(FileEntry descriptor, Chunk removeFirst, boolean compress)
	{
		super(descriptor);
		this.chunk = removeFirst;
		pleaseCompress = compress;
	}
	
	public ChunkRequest(InputStream stream) throws IOException
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
		LocalFile local = getLocal();
		if (local == null)
		{
			// lost the file...
			LogWrapper.getLogger().info("Lost file " + descriptor);
			connection.finish();
		}
		checkPermissionsDownloadable(connection, connection.getMachine(), local.getRootDirectory(), "Sending chunk.");
		ServeInstance serveInstance = Services.server.getServeInstance(connection);
		if (serveInstance == null)
		{
			LogWrapper.getLogger().info("Currently not serving on this connection.");
			serveInstance = Services.server.serve(local, connection);
			if (serveInstance == null)
			{
				connection.send(new Wait());
				connection.finish();
				return;
			}
		}
		serveInstance.serve(chunk, pleaseCompress);
	}

	public Chunk getChunk()
	{
		return chunk;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Give me chunk " + chunk);
		
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
		generator.write("pleaseCompress", pleaseCompress);
		descriptor.generate(generator, "descriptor");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needspleaseCompress = true;
		boolean needschunk = true;
		boolean needsdescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needspleaseCompress)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pleaseCompress");
				}
				if (needspleaseCompress)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pleaseCompress");
				}
				if (needschunk)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs chunk");
				}
				if (needsdescriptor)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs descriptor");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_FALSE:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("pleaseCompress")) {
					needspleaseCompress = false;
					pleaseCompress = false;
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_TRUE:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("pleaseCompress")) {
					needspleaseCompress = false;
					pleaseCompress = true;
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case START_OBJECT:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "chunk":
					needschunk = false;
					chunk = new Chunk(parser);
					break;
				case "descriptor":
					needsdescriptor = false;
					descriptor = new FileEntry(parser);
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "ChunkRequest"; }
	public String getJsonKey() { return getJsonName(); }
	public ChunkRequest(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
