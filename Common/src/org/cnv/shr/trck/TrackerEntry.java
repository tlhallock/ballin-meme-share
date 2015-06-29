
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



package org.cnv.shr.trck;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;

public class TrackerEntry extends TrackObject
{
	private String url;
	private int begin;
	private int end;
	private boolean storesMetaData = true;
	
	public static int TRACKER_PORT_BEGIN = 7006;
	public static int TRACKER_PORT_END   = 7010;
  public static int MACHINE_PAGE_SIZE  = 50;
	
  @MyParserNullable
	private Boolean sync;
	
	public TrackerEntry(String url, int portB, int portE, boolean storesMetaData)
	{
		this.url = url;
		this.begin = portB;
		this.end = portE;
		this.storesMetaData = storesMetaData;
	}
	
	public TrackerEntry(TrackerEntry entry)
	{
		this.url = entry.url;
		this.begin = entry.begin;
		this.end = entry.end;
		this.storesMetaData = entry.storesMetaData;
	}
	
	public TrackerEntry() {}

	public boolean equals(Object other)
	{
		if (!(other instanceof TrackerEntry))
		{
			return false;
		}
		
		TrackerEntry o = (TrackerEntry) other;
		return o.url.equals(url) && o.begin == begin && o.end == end;
	}
	
	public void set(String url, int begin, int end, boolean storesMetadata)
	{
		this.url = url;
		this.begin = begin;
		this.end = end;
		this.storesMetaData = storesMetadata;
	}
	
	public String getIp()
	{
		return url;
	}

	public int getBeginPort()
	{
		return begin;
	}
	
	public int getEndPort()
	{
		return end;
	}

	public String getAddress()
	{
		return url + ":" + begin + "-" + end;
	}
	
	public boolean shouldSync()
	{
		return sync != null && sync;
	}
	
	public void setSync(boolean val)
	{
		sync = val;
	}

	public boolean supportsMetaData()
	{
		return storesMetaData;
	}

	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("url", url);
		generator.write("begin", begin);
		generator.write("end", end);
		generator.write("storesMetaData", storesMetaData);
		if (sync!=null)
		generator.write("sync", sync);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsstoresMetaData = true;
		boolean needsurl = true;
		boolean needsbegin = true;
		boolean needsend = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsstoresMetaData)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs storesMetaData");
				}
				if (needsstoresMetaData)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs storesMetaData");
				}
				if (needsurl)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs url");
				}
				if (needsbegin)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs begin");
				}
				if (needsend)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs end");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_FALSE:
			if (key==null) break;
			switch(key) {
			case "storesMetaData":
				needsstoresMetaData = false;
				storesMetaData = false;
				break;
			case "sync":
				sync = false;
				break;
			}
			break;
		case VALUE_TRUE:
			if (key==null) break;
			switch(key) {
			case "storesMetaData":
				needsstoresMetaData = false;
				storesMetaData = true;
				break;
			case "sync":
				sync = true;
				break;
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("url")) {
				needsurl = false;
				url = parser.getString();
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "begin":
				needsbegin = false;
				begin = Integer.parseInt(parser.getString());
				break;
			case "end":
				needsend = false;
				end = Integer.parseInt(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "TrackerEntry"; }
	public String getJsonKey() { return getJsonName(); }
	public TrackerEntry(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
