
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



package org.cnv.shr.dmn;

import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.KeyPairObject;


public class JsonableUpdateInfo implements Jsonable
{
	private String ip;
	private int port;
	
	@MyParserNullable
	private PublicKey pKey;
	
	JsonableUpdateInfo()
	{
		ip = "127.0.0.1";
		port = org.cnv.shr.updt.UpdateInfo.DEFAULT_UPDATE_PORT;
		pKey = null;
	}
	
	String getIp()
	{
		return ip;
	}
	
	int getPort()
	{
		return port;
	}
	
	PublicKey getKey()
	{
		return pKey;
	}
	public void setAddress(String ip, int port)
	{
		this.ip = ip;
		this.port = port;
	}

	public synchronized void updateInfo(String ip, int port, PublicKey pKey)
	{
		this.ip = ip;
		this.port = port;
		this.pKey = pKey;
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("ip", ip);
		generator.write("port", port);
		if (pKey!=null)
		generator.write("pKey", KeyPairObject.serialize(pKey));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsport = true;
		boolean needsip = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsport)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs port");
				}
				if (needsip)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ip");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("port")) {
				needsport = false;
				port = Integer.parseInt(parser.getString());
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ip":
				needsip = false;
				ip = parser.getString();
				break;
			case "pKey":
				pKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "JsonableUpdateInfo"; }
	public String getJsonKey() { return getJsonName(); }
	public JsonableUpdateInfo(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}