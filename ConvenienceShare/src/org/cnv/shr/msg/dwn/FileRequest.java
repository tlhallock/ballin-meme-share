
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

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class FileRequest extends DownloadMessage
{
	private long chunkSize;

	public FileRequest(RemoteFile remoteFile, long chunkSize)
	{
		super(remoteFile.getFileEntry());
		this.chunkSize = chunkSize;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I wanna download " + getDescriptor() + " in chunksizes " + chunkSize);
		
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LocalFile local = getLocal();
		
		if (local == null)
		{
//			fail("Unable to find local file.");
			LogWrapper.getLogger().info("Unable to file file " + descriptor);
			connection.finish();
		}
		if (local.getFileSize() == 0)
		{
			LogWrapper.getLogger().info("Not serving zero length file: " + local);
			connection.finish();
			return;
		}
		
		checkPermissionsDownloadable(connection, connection.getMachine(), local.getRootDirectory(), "Serve file");
		ServeInstance serve = Services.server.serve(local, connection);
		if (serve == null)
		{
			return;
		}
		
		synchronized (serve)
		{
			serve.sendChunks(chunkSize);
		}
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("chunkSize", chunkSize);
		descriptor.generate(generator, "descriptor");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsDescriptor = true;
		boolean needsChunkSize = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsDescriptor)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.dwn.DownloadMessage\" needs \"descriptor\"");
				}
				if (needsChunkSize)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.dwn.FileRequest\" needs \"chunkSize\"");
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
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("chunkSize")) {
					needsChunkSize = false;
					chunkSize = Long.parseLong(parser.getString());
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "FileRequest"; }
	public String getJsonKey() { return getJsonName(); }
	public FileRequest(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
