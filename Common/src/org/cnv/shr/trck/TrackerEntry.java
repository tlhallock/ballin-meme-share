
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

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;

public class TrackerEntry extends TrackObject
{
	private String url;
	private int begin;
	private int end;
	public static int TRACKER_PORT_END   = 9005;
	public static int TRACKER_PORT_BEGIN = 9001;
  public static int MACHINE_PAGE_SIZE  = 50;
	
  @MyParserNullable
	private Boolean sync;
	
	public TrackerEntry(String url, int portB, int portE)
	{
		this.url = url;
		this.begin = portB;
		this.end = portE;
	}
	
	public TrackerEntry(TrackerEntry entry)
	{
		this.url = entry.url;
		this.begin = entry.begin;
		this.end = entry.end;
	}

	public TrackerEntry() {}

//	@Override
//	public void read(JsonParser parser)
//	{
//		String key = null;
//		while (parser.hasNext())
//		{
//			JsonParser.Event e = parser.next();
//			switch (e)
//			{
//			case KEY_NAME:
//				key = parser.getString();
//				break;
//			case VALUE_STRING:
//				if (key == null) break;
//				switch (key)
//				{
//				case "url":    url      = parser.getString();  break;
//				}
//				break;
//			case VALUE_NUMBER:
//				if (key == null) break;
//				BigDecimal bd = new BigDecimal(parser.getString());
//				switch (key)
//				{
//				case "beginPort": begin = bd.intValue(); break;
//				case "endPort":   end   = bd.intValue(); break;
//				}
//				break;
//			case VALUE_FALSE:
//				switch(key)
//				{
//				case "sync" : sync = false; break;
//				}
//				break;
//			case VALUE_TRUE:
//				switch(key)
//				{
//				case "sync" : sync = true; break;
//				}
//				break;
//			case END_OBJECT:
//				return;
//			default:
//				break;
//			}
//		}
//	}
//
//	@Override
//	public void print(JsonGenerator generator)
//	{
//		generator.writeStartObject();
//		generator.write("url", url);
//		generator.write("beginPort", begin);
//		generator.write("endPort", end);
//		if (sync != null)
//			generator.write("sync", sync);
//		generator.writeEnd();
//	}
	
	public void set(String url, int begin, int end)
	{
		this.url = url;
		this.begin = begin;
		this.end = end;
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
		if (sync!=null)
		generator.write("sync", sync);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsbegin = true;
		boolean needsend = true;
		boolean needsurl = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsbegin)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs begin");
				}
				if (needsend)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs end");
				}
				if (needsurl)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs url");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_FALSE:
			if (key==null) break;
			if (key.equals("sync")) {
				sync = false;
			}
			break;
		case VALUE_TRUE:
			if (key==null) break;
			if (key.equals("sync")) {
				sync = true;
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
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("url")) {
				needsurl = false;
				url = parser.getString();
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "TrackerEntry"; }
	public String getJsonKey() { return getJsonName(); }
	public TrackerEntry(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
