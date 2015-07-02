
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



package org.cnv.shr.dmn.dwn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public class Chunk implements Jsonable
{
	// Should this be nullable?
	@MyParserNullable
	private String checksum;
	
	private long begin;
	private long end;
	
	Chunk(String value)
	{
		try (Scanner s = new Scanner(value);)
		{
			begin = s.nextLong();
			end = s.nextLong();
			checksum = s.next();
		}
	}
	
	public Chunk(long begin, long end, String checksum)
	{
		this.begin = begin;
		this.end = end;
		this.checksum = checksum;
		if (begin > end)
		{
			throw new IllegalArgumentException("Bad begin/end: "  + begin + " - " + end);
		}
	}

	public Chunk(ByteReader input) throws IOException
	{
		read(input);
	}

	public void read(ByteReader input) throws IOException
	{
		begin =  input.readLong();
		end = input.readLong();
		checksum = input.readString();
	}

	public void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(getBegin());
		buffer.append(end);
		buffer.append(checksum == null ? "" : checksum);
	}
	
	@Override
	public String toString()
	{
		return getBegin() + " " + end + " " + checksum;
	}

	public void setChecksum(String checksum2)
	{
		this.checksum = checksum2;
	}

	public long getEnd()
	{
		return end;
	}

	public long getBegin()
	{
		return begin;
	}

	public String getChecksum()
	{
		return checksum;
	}

	public long getSize()
	{
		return end - getBegin();
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof Chunk))
		{
			return false;
		}
		
		Chunk other = (Chunk) o;
		return checksum.equals(other.checksum)
				&& getBegin() == other.getBegin() 
				&& end == other.end;
	}
	
	public int hashCode()
	{
		return toString().hashCode();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		if (checksum!=null)
		generator.write("checksum", checksum);
		generator.write("begin", begin);
		generator.write("end", end);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsbegin = true;
		boolean needsend = true;
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
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "begin":
					needsbegin = false;
					begin = Long.parseLong(parser.getString());
					break;
				case "end":
					needsend = false;
					end = Long.parseLong(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("checksum")) {
					checksum = parser.getString();
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "Chunk"; }
	public String getJsonKey() { return getJsonName(); }
	public Chunk(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
