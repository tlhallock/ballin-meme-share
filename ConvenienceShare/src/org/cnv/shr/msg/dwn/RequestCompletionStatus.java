
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
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class RequestCompletionStatus extends DownloadMessage
{
	public RequestCompletionStatus(FileEntry descriptor) { super (descriptor); }
	
	public RequestCompletionStatus(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		for (DownloadInstance instance : Services.downloads.getDownloadInstances(connection))
		{
			instance.sendCompletionStatus();
		}
	}

	public static int TYPE = 20;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Are you done yet?");
		return builder.toString();
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException {}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException {}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		descriptor.generate(generator, "descriptor");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdescriptor)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs descriptor");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("descriptor")) {
				needsdescriptor = false;
				descriptor = new FileEntry(parser);
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "RequestCompletionStatus"; }
	public String getJsonKey() { return getJsonName(); }
	public RequestCompletionStatus(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
