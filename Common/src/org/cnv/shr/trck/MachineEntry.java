
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
import org.cnv.shr.util.KeyPairObject;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class MachineEntry extends TrackObject
{
	private String ident;
	@MyParserNullable
	private String keyStr;
	private String name;
	
	private String ip;
	private int beginPort;
	private int endPort;
	
	public MachineEntry() {}

	public MachineEntry(String ident, RSAPublicKey key, String ip, int beginPort, int endPort, String name)
	{
		this(ident, KeyPairObject.serialize(key), ip, beginPort, endPort, name);
	}
	
	public MachineEntry(String ident, String key, String ip, int beginPort, int endPort, String name)
	{
		this.ident = ident;
		this.keyStr = key;
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
		this.name = name;
	}

	public RSAPublicKey getKey()
	{
		return KeyPairObject.deSerializePublicKey(keyStr);
	}
	
	public String getKeyStr()
	{
		return keyStr;
	}
	
	public void set(String ident, String key, String ip, int beginPort, int endPort, String name)
	{
		this.ident = ident;
		this.keyStr = key;
		this.ip = ip;
		this.beginPort = beginPort;
		this.endPort = endPort;
		this.name = name;
	}

	public String getIdentifer()
	{
		return ident;
	}

	public int getPortEnd()
	{
		return Math.min(endPort, beginPort + 1);
	}

	public int getPortBegin()
	{
		return beginPort;
	}

	public String getIp()
	{
		return ip;
	}

	public String getName()
	{
		return name;
	}

	public String getAddress()
	{
		return ip + ":" + beginPort + "-" + endPort;
	}

	public void setIp(String realAddress)
	{
		this.ip = realAddress;
	}

	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("ident", ident);
		if (keyStr!=null)
		generator.write("keyStr", keyStr);
		generator.write("name", name);
		generator.write("ip", ip);
		generator.write("beginPort", beginPort);
		generator.write("endPort", endPort);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsbeginPort = true;
		boolean needsendPort = true;
		boolean needsident = true;
		boolean needsname = true;
		boolean needsip = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsbeginPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs beginPort");
				}
				if (needsendPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs endPort");
				}
				if (needsident)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ident");
				}
				if (needsname)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
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
			switch(key) {
			case "beginPort":
				needsbeginPort = false;
				beginPort = Integer.parseInt(parser.getString());
				break;
			case "endPort":
				needsendPort = false;
				endPort = Integer.parseInt(parser.getString());
				break;
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ident":
				needsident = false;
				ident = parser.getString();
				break;
			case "keyStr":
				keyStr = parser.getString();
				break;
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "ip":
				needsip = false;
				ip = parser.getString();
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "MachineEntry"; }
	public String getJsonKey() { return getJsonName(); }
	public MachineEntry(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
