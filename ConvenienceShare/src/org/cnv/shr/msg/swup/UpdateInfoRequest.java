
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



package org.cnv.shr.msg.swup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.Misc;

public class UpdateInfoRequest extends Message
{
	public static final int TYPE = 38;
	
	private PublicKey publicKey;
	private byte[] naunceRequest;
	
	public UpdateInfoRequest(InputStream input) throws IOException
	{
		super(input);
	}
	
	public UpdateInfoRequest(PublicKey pKey, byte[] encrypted)
	{
		this.publicKey = pKey;
		this.naunceRequest = encrypted;
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		naunceRequest = reader.readVarByteArray();
		publicKey = reader.readPublicKey();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(naunceRequest);
		buffer.append(publicKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (Services.codeUpdateInfo == null)
		{
			connection.finish();
			return;
		}
		byte[] decrypted = Services.keyManager.decrypt(Services.codeUpdateInfo.getPrivateKey(publicKey), naunceRequest);
		connection.send(new UpdateInfoMessage(decrypted));
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("publicKey", KeyPairObject.serialize(publicKey));
		generator.write("naunceRequest", Misc.format(naunceRequest));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needspublicKey = true;
		boolean needsnaunceRequest = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needspublicKey)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs publicKey");
				}
				if (needsnaunceRequest)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs naunceRequest");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "publicKey":
				needspublicKey = false;
				publicKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "naunceRequest":
				needsnaunceRequest = false;
				naunceRequest = Misc.format(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "UpdateInfoRequest"; }
	public String getJsonKey() { return getJsonName(); }
	public UpdateInfoRequest(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
