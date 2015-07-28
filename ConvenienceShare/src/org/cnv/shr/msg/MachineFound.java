
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



package org.cnv.shr.msg;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.HandShake;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class MachineFound extends Message
{
	protected String ip;
	protected int port;
	protected String name;
	protected String ident;
	@MyParserIgnore
	private long lastActive;

	public MachineFound()
	{
		this(Services.localMachine);
		Objects.requireNonNull(ip);
	}
	
	public MachineFound(Machine m)
	{
		ip         = m.getIp();
		port       = m.getPort();
		name       = m.getName();
		ident      = m.getIdentifier();
		lastActive = m.getLastActive();
		Objects.requireNonNull(ip);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (ident.equals(Services.localMachine.getIdentifier()))
		{
			LogWrapper.getLogger().info("We already know about the local machine.");
			return;
		}
		if (!HandShake.verifyMachine(ident, ip, port, name, null))
		{
			LogWrapper.getLogger().info("A machine at " + ip + " already exists.");
			return;
		}
		DbMachines.updateMachineInfo(
				ident,
				name,
				null,
				ip,
				port);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("There is a machine with ident=" + ident + " at " + ip + ":" + port);
		
		return builder.toString();
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
		generator.write("name", name);
		generator.write("ident", ident);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsPort = true;
		boolean needsIp = true;
		boolean needsName = true;
		boolean needsIdent = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsPort)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.MachineFound\" needs \"port\"");
				}
				if (needsIp)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.MachineFound\" needs \"ip\"");
				}
				if (needsName)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.MachineFound\" needs \"name\"");
				}
				if (needsIdent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.MachineFound\" needs \"ident\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("port")) {
					needsPort = false;
					port = Integer.parseInt(parser.getString());
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "ip":
					needsIp = false;
					ip = parser.getString();
					break;
				case "name":
					needsName = false;
					name = parser.getString();
					break;
				case "ident":
					needsIdent = false;
					ident = parser.getString();
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "MachineFound"; }
	public String getJsonKey() { return getJsonName(); }
	public MachineFound(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
