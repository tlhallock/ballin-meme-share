
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

import java.util.HashMap;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.json.JsonStringMap;

public class TrackerRequest extends TrackObject
{
	private String action;
	private JsonStringMap params = new JsonStringMap();

	public TrackerRequest() {}
	
	public TrackerRequest(TrackerAction action)
	{
		this.action = action.name();
	}

	public void setParameter(String name, String value)
	{
		params.put(name, value);
	}

	public void set(TrackerAction action)
	{
		this.action = action.name();
		params.clear();
	}

	public TrackerAction getAction()
	{
		return TrackerAction.valueOf(action);
	}

	public String getParam(String string)
	{
		return params.get(string);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("action", action);
		{
			generator.writeStartObject("params");
			params.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsaction = true;
		boolean needsparams = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsaction)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs action");
				}
				if (needsparams)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs params");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("action")) {
				needsaction = false;
				action = parser.getString();
			}
			break;
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("params")) {
				needsparams = false;
				params.parse(parser);
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "TrackerRequest"; }
	public String getJsonKey() { return getJsonName(); }
	public TrackerRequest(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
