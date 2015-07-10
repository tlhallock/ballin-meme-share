
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
import java.io.InputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class UpdateInfoRequestRequest extends Message
{
	public static final int TYPE = 37;
	private String action;
	
	public UpdateInfoRequestRequest(String action)
	{
		this.action = action;
	}

	public UpdateInfoRequestRequest(InputStream input) throws IOException
	{
		super(input);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		byte[] pending = Misc.createNaunce(Services.settings.minNaunce.get());
		connection.getAuthentication().addPendingNaunce(pending);
		PublicKey publicKey = Services.updateManager.getPublicKey();
		byte[] encrypted = Services.keyManager.encrypt(publicKey, pending);
		connection.send(new UpdateInfoRequest(publicKey, encrypted, action));
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("action", action);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsAction = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsAction)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs action");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("action")) {
					needsAction = false;
					action = parser.getString();
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "UpdateInfoRequestRequest"; }
	public String getJsonKey() { return getJsonName(); }
	public UpdateInfoRequestRequest(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
