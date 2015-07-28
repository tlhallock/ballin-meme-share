
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

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;


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
		Machine remoteMachine = DbMachines.getMachine(machine);
		if (remoteMachine == null)
		{
			LogWrapper.getLogger().info("Unable to get remote machine " + machine);
			return;
		}

		UserMessage userMessage = new UserMessage(remoteMachine, UserMessage.MessageType.valueOf(messageType).getDbValue(), message, sent);
		try
		{
			userMessage.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save user message " + this, e);
		}
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
		boolean needsSent = true;
		boolean needsMachine = true;
		boolean needsMessageType = true;
		boolean needsMessage = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsSent)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.MessageBackup\" needs \"sent\"");
				}
				if (needsMachine)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.MessageBackup\" needs \"machine\"");
				}
				if (needsMessageType)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.MessageBackup\" needs \"messageType\"");
				}
				if (needsMessage)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.MessageBackup\" needs \"message\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("sent")) {
					needsSent = false;
					sent = Long.parseLong(parser.getString());
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "machine":
					needsMachine = false;
					machine = parser.getString();
					break;
				case "messageType":
					needsMessageType = false;
					messageType = parser.getString();
					break;
				case "message":
					needsMessage = false;
					message = parser.getString();
					break;
				default: LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "MessageBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public MessageBackup(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
