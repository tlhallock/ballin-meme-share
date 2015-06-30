
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class UpdateInfoMessage extends Message
{
	public static int TYPE = 39;
	
	private String ip;
	private int port;
	private PublicKey pKey;
	byte[]  decryptedNaunce;

	public UpdateInfoMessage(byte[] decrypted)
	{
		decryptedNaunce = decrypted;
		ip = Services.codeUpdateInfo.getIp();
		port = Services.codeUpdateInfo.getPort();
		pKey = Services.codeUpdateInfo.getLatestPublicKey();
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		ip = reader.readString();
		port = reader.readInt();
		pKey = reader.readPublicKey();
		decryptedNaunce = reader.readVarByteArray();
	}
	
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(pKey);
		buffer.appendVarByteArray(decryptedNaunce);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (!connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			LogWrapper.getLogger().info("Update server machine failed authentication.");
			connection.finish();
			return;
		}

		connection.finish();
		
		Services.updateManager.updateInfo(ip, port, pKey);
		Services.updateManager.checkForUpdates(null, true);
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
		generator.write("pKey", KeyPairObject.serialize(pKey));
		generator.write("decryptedNaunce", Misc.format(decryptedNaunce));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsport = true;
		boolean needsip = true;
		boolean needspKey = true;
		boolean needsdecryptedNaunce = true;
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
				if (needspKey)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pKey");
				}
				if (needsdecryptedNaunce)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs decryptedNaunce");
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
				needspKey = false;
				pKey = KeyPairObject.deSerializePublicKey(parser.getString());
				break;
			case "decryptedNaunce":
				needsdecryptedNaunce = false;
				decryptedNaunce = Misc.format(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "UpdateInfoMessage"; }
	public String getJsonKey() { return getJsonName(); }
	public UpdateInfoMessage(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
