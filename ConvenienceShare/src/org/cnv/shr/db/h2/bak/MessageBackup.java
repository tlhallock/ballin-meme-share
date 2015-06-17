
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

package org.cnv.shr.db.h2.bak;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.util.Jsonable;


public class MessageBackup implements Jsonable
{
	private String machine;
	private String messageType;
	private String message;
	private long sent;
	
	
	public MessageBackup(UserMessage message)
	{
		machine = message.getMachine().getIdentifier();
		messageType = message.getMessageType().name();
		this.message = message.getMessage();
		sent = message.getSent();
	}

	public void save(ConnectionWrapper wrapper)
	{
		// ...
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("machine", machine);
		generator.write("messageType", messageType);
		generator.write("message", message);
		generator.write("sent", sent);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needssent = true;
		boolean needsmachine = true;
		boolean needsmessageType = true;
		boolean needsmessage = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needssent)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs sent");
				}
				if (needsmachine)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs machine");
				}
				if (needsmessageType)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs messageType");
				}
				if (needsmessage)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs message");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("sent")) {
				needssent = false;
				sent = Long.parseLong(parser.getString());
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "machine":
				needsmachine = false;
				machine = parser.getString();
				break;
			case "messageType":
				needsmessageType = false;
				messageType = parser.getString();
				break;
			case "message":
				needsmessage = false;
				message = parser.getString();
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "MessageBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public MessageBackup(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
