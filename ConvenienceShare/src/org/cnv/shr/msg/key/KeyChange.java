
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
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class KeyChange extends KeyMessage
{
	private PublicKey oldKey;
	private PublicKey newKey;
	private byte[] decryptedProof;
	private byte[] naunceRequest;

	public KeyChange(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public KeyChange(PublicKey oldKey, PublicKey newKey, byte[] deryptedProof, byte[] naunceRequest)
	{
		this.oldKey = oldKey;
		this.newKey = newKey;
		this.decryptedProof = deryptedProof;
		this.naunceRequest = naunceRequest;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		oldKey         = reader.readPublicKey();
		newKey         = reader.readPublicKey();
		decryptedProof = reader.readVarByteArray();
		naunceRequest  = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(oldKey        );
		buffer.append(newKey        );
		buffer.appendVarByteArray(decryptedProof);
		buffer.appendVarByteArray(naunceRequest );
		
	}

	public static final int TYPE = 27;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine machine = connection.getMachine();
		if (DbKeys.machineHasKey(machine, oldKey) && connection.getAuthentication().hasPendingNaunce(decryptedProof))
		{
			DbKeys.addKey(machine, newKey);
			connection.getAuthentication().setRemoteKey(newKey);
			connection.getAuthentication().authenticateToTarget(connection, naunceRequest);
			return;
		}
		fail("Key change: did not know key.", connection);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("oldKey", KeyPairObject.serialize(oldKey));
		generator.write("newKey", KeyPairObject.serialize(newKey));
		generator.write("decryptedProof", Misc.format(decryptedProof));
		generator.write("naunceRequest", Misc.format(naunceRequest));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsOldKey = true;
		boolean needsNewKey = true;
		boolean needsDecryptedProof = true;
		boolean needsNaunceRequest = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsOldKey)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.key.KeyChange\" needs \"oldKey\"");
				}
				if (needsNewKey)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.key.KeyChange\" needs \"newKey\"");
				}
				if (needsDecryptedProof)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.key.KeyChange\" needs \"decryptedProof\"");
				}
				if (needsNaunceRequest)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.key.KeyChange\" needs \"naunceRequest\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "oldKey":
					needsOldKey = false;
					oldKey = KeyPairObject.deSerializePublicKey(parser.getString());
					break;
				case "newKey":
					needsNewKey = false;
					newKey = KeyPairObject.deSerializePublicKey(parser.getString());
					break;
				case "decryptedProof":
					needsDecryptedProof = false;
					decryptedProof = Misc.format(parser.getString());
					break;
				case "naunceRequest":
					needsNaunceRequest = false;
					naunceRequest = Misc.format(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "KeyChange"; }
	public String getJsonKey() { return getJsonName(); }
	public KeyChange(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
