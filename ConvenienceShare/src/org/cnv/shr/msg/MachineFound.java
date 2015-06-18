
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class MachineFound extends Message
{
	protected String ip;
	protected int port;
	protected int nports;
	protected String name;
	protected String ident;
	@MyParserIgnore
	private long lastActive;
	
	public MachineFound(InputStream stream) throws IOException
	{
		super(stream);
	}

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
		nports     = m.getNumberOfPorts();
		Objects.requireNonNull(ip);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (ident.equals(Services.localMachine.getIdentifier()))
		{
			return;
		}
		DbMachines.updateMachineInfo(
				ident,
				name,
				null,
				ip,
				port,
				nports);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		ip          = reader.readString();
		port        = reader.readInt();
		name        = reader.readString();
		ident       = reader.readString();
		lastActive  = reader.readLong();
		nports      = reader.readInt();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(name);
		buffer.append(ident);
		buffer.append(lastActive);
		buffer.append(nports);
	}

	public static int TYPE = 18;
	@Override
	protected int getType()
	{
		return TYPE;
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
		generator.write("nports", nports);
		generator.write("name", name);
		generator.write("ident", ident);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsip = true;
		boolean needsname = true;
		boolean needsident = true;
		boolean needsport = true;
		boolean needsnports = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsip)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ip");
				}
				if (needsname)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
				}
				if (needsident)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ident");
				}
				if (needsport)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs port");
				}
				if (needsnports)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs nports");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ip":
				needsip = false;
				ip = parser.getString();
				break;
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "ident":
				needsident = false;
				ident = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "port":
				needsport = false;
				port = Integer.parseInt(parser.getString());
				break;
			case "nports":
				needsnports = false;
				nports = Integer.parseInt(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "MachineFound"; }
	public String getJsonKey() { return getJsonName(); }
	public MachineFound(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
