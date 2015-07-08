
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
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.updt.UpdateInfo;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

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
		pKey          = Services.keyManager.getPublicKey();
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
		if (Services.blackList.contains(ident))
		{
			LogWrapper.getLogger().info(ident + " is a blacklisted machine.");
			connection.finish();
			return;
		}
		
		if (UserActions.checkIfMachineShouldNotReplaceOld(ident, connection.getIp(), port))
		{
			throw new RuntimeException("A different machine at " + connection.getIp() + " already exists");
		}
		
		connection.setRemoteIdentifier(ident);
		connection.getAuthentication().setMachineInfo(name, port, nports);
		connection.getAuthentication().offerRemote(ident, connection.getIp(), pKey);
		
		UpdateInfo codeUpdateInfo = Services.codeUpdateInfo;
		if (codeUpdateInfo != null)
		{
			// TODO: Should wait until authenticated...
			Machine machine = connection.getMachine();
			if (machine != null)
			{
				codeUpdateInfo.setLastKnownVersion(machine.getIdentifier(), versionString);
			}
		}
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
		boolean needsPKey = true;
		boolean needsVersionString = true;
		boolean needsIp = true;
		boolean needsName = true;
		boolean needsIdent = true;
		boolean needsPort = true;
		boolean needsNports = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsPKey)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pKey");
				}
				if (needsVersionString)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs versionString");
				}
				if (needsIp)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ip");
				}
				if (needsName)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
				}
				if (needsIdent)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ident");
				}
				if (needsPort)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs port");
				}
				if (needsNports)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs nports");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "pKey":
					needsPKey = false;
					pKey = KeyPairObject.deSerializePublicKey(parser.getString());
					break;
				case "versionString":
					needsVersionString = false;
					versionString = parser.getString();
					break;
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
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_NUMBER:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "port":
					needsPort = false;
					port = Integer.parseInt(parser.getString());
					break;
				case "nports":
					needsNports = false;
					nports = Integer.parseInt(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
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
