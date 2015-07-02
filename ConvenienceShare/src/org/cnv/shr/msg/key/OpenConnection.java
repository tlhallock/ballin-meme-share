
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
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class OpenConnection extends KeyMessage
{
	public static int TYPE = 0;
	
	private PublicKey sourcePublicKey;
	@MyParserNullable
	private PublicKey destinationPublicKey;
	private byte[] requestedNaunce;

	public OpenConnection(InputStream stream) throws IOException
	{
		super(stream);
	}
	public OpenConnection(PublicKey remotePublicKey, byte[] requestedNaunce)
	{
		destinationPublicKey = remotePublicKey;
		this.requestedNaunce = requestedNaunce;
		sourcePublicKey = Services.keyManager.getPublicKey();
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		sourcePublicKey      = reader.readPublicKey();
		destinationPublicKey = reader.readPublicKey();
		requestedNaunce      = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(sourcePublicKey);
		buffer.append(destinationPublicKey);
		buffer.appendVarByteArray(requestedNaunce);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().canAuthenticateRemote(connection, sourcePublicKey, destinationPublicKey))
		{
			connection.getAuthentication().authenticateToTarget(connection, requestedNaunce);
			return;
		}

		PublicKey[] knownKeys = DbKeys.getKeys(connection.getMachine());
		if (knownKeys != null && knownKeys.length > 0)
		{
			LogWrapper.getLogger().info("We have a different key for the remote.");
			connection.send(new KeyNotFound(connection, knownKeys));
			return;
		}
		
		fail("Open connection: has keys, but not claimed keys.", connection);
	}
	
	@Override
	public String toString()
	{
		return "Please open a connection to me. my key=" + sourcePublicKey + " your key= " + destinationPublicKey;
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("sourcePublicKey", KeyPairObject.serialize(sourcePublicKey));
		if (destinationPublicKey!=null)
		generator.write("destinationPublicKey", KeyPairObject.serialize(destinationPublicKey));
		generator.write("requestedNaunce", Misc.format(requestedNaunce));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needssourcePublicKey = true;
		boolean needsrequestedNaunce = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needssourcePublicKey)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs sourcePublicKey");
				}
				if (needsrequestedNaunce)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs requestedNaunce");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "sourcePublicKey":
					needssourcePublicKey = false;
					sourcePublicKey = KeyPairObject.deSerializePublicKey(parser.getString());
					break;
				case "destinationPublicKey":
					destinationPublicKey = KeyPairObject.deSerializePublicKey(parser.getString());
					break;
				case "requestedNaunce":
					needsrequestedNaunce = false;
					requestedNaunce = Misc.format(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "OpenConnection"; }
	public String getJsonKey() { return getJsonName(); }
	public OpenConnection(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
