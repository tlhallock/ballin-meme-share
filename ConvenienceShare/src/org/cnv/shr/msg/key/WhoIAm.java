
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



package org.cnv.shr.msg.key;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;

public class WhoIAm extends MachineFound
{
	public static int TYPE = 29;
	
	protected PublicKey pKey;
	protected String versionString;
	
	public WhoIAm(InputStream input) throws IOException
	{
		super(input);
	}
	
	public WhoIAm()
	{
		super();
		pKey       = Services.keyManager.getPublicKey();
		versionString = Services.settings.getVersion(Main.class);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		super.parse(reader);
		versionString = reader.readString();
		pKey = reader.readPublicKey();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		super.print(connection, buffer);
		buffer.append(versionString);
		buffer.append(pKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.setRemoteIdentifier(ident);
		connection.getAuthentication().setMachineInfo(name, port, nports);
		connection.getAuthentication().offerRemote(ident, connection.getIp(), pKey);
	}

	@Override
	public boolean requiresAthentication()
	{
		return false;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("I am a machine with ident=" + ident + " on a port " + port);
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("pKey", KeyPairObject.serialize(pKey));
		generator.write("versionString", versionString);
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
		boolean needsport = true;
		boolean needsnports = true;
		boolean needspKey = true;
		boolean needsversionString = true;
		boolean needsip = true;
		boolean needsname = true;
		boolean needsident = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsport)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs port");
				}
				if (needsnports)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs nports");
				}
				if (needspKey)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pKey");
				}
				if (needsversionString)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs versionString");
				}
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
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
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
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "pKey":
				needspKey = false;
				pKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "versionString":
				needsversionString = false;
				versionString = parser.getString();
				break;
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
			default: break;
			}
		}
	}
	public static String getJsonName() { return "WhoIAm"; }
	public String getJsonKey() { return getJsonName(); }
	public WhoIAm(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
