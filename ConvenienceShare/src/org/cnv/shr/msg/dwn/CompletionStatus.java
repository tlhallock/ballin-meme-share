
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

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class CompletionStatus extends DownloadMessage
{
	public static int TYPE = 12;
	
	private double percentComplete;
	
	public CompletionStatus(FileEntry descriptor, double d)
	{
		super(descriptor);
		percentComplete = d;
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}
	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		percentComplete = reader.readDouble();
	}
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(percentComplete);
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		ServeInstance serveInstance = Services.server.getServeInstance(connection);
		if (serveInstance == null)
		{
			connection.finish();
			return;
		}
		serveInstance.setPercentComplete(percentComplete);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("remote is " + percentComplete + " done.");
		
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("percentComplete", percentComplete);
		descriptor.generate(generator, "descriptor");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsPercentComplete = true;
		boolean needsDescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsPercentComplete)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs percentComplete");
				}
				if (needsDescriptor)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs descriptor");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("percentComplete")) {
					needsPercentComplete = false;
					percentComplete = Double.parseDouble(parser.getString());
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case START_OBJECT:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("descriptor")) {
					needsDescriptor = false;
					descriptor = new FileEntry(parser);
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "CompletionStatus"; }
	public String getJsonKey() { return getJsonName(); }
	public CompletionStatus(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
