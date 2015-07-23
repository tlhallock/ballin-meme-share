
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

import org.cnv.shr.util.LogWrapper;

public class CommentEntry extends TrackObject
{
	private static final int MAX_TEXT_LENGTH = 1024;
	private static final int MAX_RATING = 10;
	
	private String originIdent;
	private String destIdent;
	private String text;
	private int rating;
	private long date;

	public CommentEntry(String originIdent, String destIdent, String text, int rating, long date)
	{
		this.originIdent = originIdent;
		this.destIdent = destIdent;
		this.text = text;
		this.rating = rating;
		this.date = date;
	}

	public CommentEntry() {}

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
//				case "oid":  originIdent = parser.getString();  break;
//				case "did":  destIdent   = parser.getString();  break;
//				case "text": text        = parser.getString();  break;
//				}
//				break;
//			case VALUE_NUMBER:
//				if (key == null) break;
//				BigDecimal bd = new BigDecimal(parser.getString());
//				switch (key)
//				{
//				case "rating": rating = bd.intValue(); break;
//				case "date":   date   = bd.longValue(); break;
//				}
//				break;
//			case END_OBJECT:
//				return;
//			default:
//			}
//		}
//	}
//
//	@Override
//	public void print(JsonGenerator generator)
//	{
//		generator.writeStartObject();
//		generator.write("oid", originIdent);
//		generator.write("did", destIdent);
//		generator.write("text", getText());
//		generator.write("rating", getRating());
//		generator.write("date", getDate());
//		generator.writeEnd();
//	}
	

	public void set(String originIdent, String destIdent, String text, int rating, long date)
	{
		this.originIdent = originIdent;
		this.destIdent = destIdent;
		this.text = text;
		this.rating = rating;
		this.date = date;
	}
	
	public String getOrigin()
	{
		return originIdent;
	}

	public String getDestination()
	{
		return destIdent;
	}

	public long getDate()
	{
		return Math.min(System.currentTimeMillis(), date);
	}

	public int getRating()
	{
		return Math.max(0, Math.min(MAX_RATING, rating));
	}

	public String getText()
	{
		if (text.length() > MAX_TEXT_LENGTH)
		{
			text = text.substring(0, MAX_TEXT_LENGTH);
		}
		return text;
	}

	public void setOrigin(String identifer)
	{
		this.originIdent = identifer;
	}

	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("originIdent", originIdent);
		generator.write("destIdent", destIdent);
		generator.write("text", text);
		generator.write("rating", rating);
		generator.write("date", date);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsOriginIdent = true;
		boolean needsDestIdent = true;
		boolean needsText = true;
		boolean needsRating = true;
		boolean needsDate = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsOriginIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.trck.CommentEntry\" needs \"originIdent\"");
				}
				if (needsDestIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.trck.CommentEntry\" needs \"destIdent\"");
				}
				if (needsText)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.trck.CommentEntry\" needs \"text\"");
				}
				if (needsRating)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.trck.CommentEntry\" needs \"rating\"");
				}
				if (needsDate)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.trck.CommentEntry\" needs \"date\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "originIdent":
					needsOriginIdent = false;
					originIdent = parser.getString();
					break;
				case "destIdent":
					needsDestIdent = false;
					destIdent = parser.getString();
					break;
				case "text":
					needsText = false;
					text = parser.getString();
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "rating":
					needsRating = false;
					rating = Integer.parseInt(parser.getString());
					break;
				case "date":
					needsDate = false;
					date = Long.parseLong(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "CommentEntry"; }
	public String getJsonKey() { return getJsonName(); }
	public CommentEntry(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
