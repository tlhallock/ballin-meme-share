
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



package org.cnv.shr.db.h2;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;

public enum SharingState implements Jsonable
	{
		DO_NOT_SHARE(1, false, false),
		SHARE_PATHS (2,  true, false),
		DOWNLOADABLE(3,  true,  true),
		;
		@MyParserIgnore
		boolean canList;
		@MyParserIgnore
		boolean canDownload;
		
		int state;
		
		SharingState(int i, boolean cl, boolean cd)
		{
			this.state = i;
			this.canList = cl;
			this.canDownload = cd;
		}
		
		public String humanReadable()
		{
			return name();
		}
		
		public boolean is(int i)
		{
			return state == i;
		}
		
		public static SharingState get(int dbValue)
		{
			for (SharingState s : values())
			{
				if (s.state == dbValue)
				{
					return s;
				}
			}
			return null;
		}

		public int getDbValue()
		{
			return state;
		}

		public boolean downloadable()
		{
			return canDownload;
		}

		public boolean listable()
		{
			return canList;
		}
		
		public boolean isLessOrEquallyRestriveThan(SharingState other)
		{
			return state >= other.state;
//			switch (this)
//			{
//				case DOWNLOADABLE: return true;
//				case SHARE_PATHS:  return other.equals(SHARE_PATHS) || other.equals(DO_NOT_SHARE);
//				case DO_NOT_SHARE: return other.equals(DO_NOT_SHARE);
//				default:           return false;
//			}
		}
		
		public boolean isMoreRestrictiveThan(SharingState other)
		{
			return !isLessOrEquallyRestriveThan(other);
		}
		
		// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("state", state);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsstate = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsstate)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs state");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("state")) {
					needsstate = false;
					state = Integer.parseInt(parser.getString());
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "SharingState"; }
	public String getJsonKey() { return getJsonName(); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
		// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	}
