//
///*                                                                          *
// * Copyright (C) 2015    Trever Hallock                                     *
// *                                                                          *
// * This program is free software; you can redistribute it and/or modify     *
// * it under the terms of the GNU General Public License as published by     *
// * the Free Software Foundation; either version 2 of the License, or        *
// * (at your option) any later version.                                      *
// *                                                                          *
// * This program is distributed in the hope that it will be useful,          *
// * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
// * GNU General Public License for more details.                             *
// *                                                                          *
// * You should have received a copy of the GNU General Public License along  *
// * with this program; if not, write to the Free Software Foundation, Inc.,  *
// * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
// *                                                                          *
// * See LICENSE file at repo head at                                         *
// * https://github.com/tlhallock/ballin-meme-share                           *
// * or after                                                                 *
// * git clone git@github.com:tlhallock/ballin-meme-share.git                 */
//
//
//
//package org.cnv.shr.msg.key;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//import javax.json.stream.JsonGenerator;
//import javax.json.stream.JsonParser;
//
//import org.cnv.shr.cnctn.Communication;
//import org.cnv.shr.trck.TrackObjectUtils;
//import org.cnv.shr.util.AbstractByteWriter;
//import org.cnv.shr.util.ByteReader;
//import org.cnv.shr.util.LogWrapper;
//
//public class KeyFailure extends KeyMessage
//{
//	private String reason;
//	
//	public KeyFailure(String reason)
//	{
//		this.reason = reason;
//	}
//	public KeyFailure(InputStream stream) throws IOException
//	{
//		super(stream);
//	}
//	
//	@Override
//	protected void parse(ByteReader reader) throws IOException {}
//
//	@Override
//	protected void print(Communication connection, AbstractByteWriter buffer) {}
//
//	public static int TYPE = 26;
//	@Override
//	protected int getType()
//	{
//		return TYPE;
//	}
//	
//	@Override
//	public String toString()
//	{
//		return "Unable to authenticate: " + reason;
//	}
//
//	@Override
//	public void perform(Communication connection) throws Exception
//	{
//		LogWrapper.getLogger().info("Key failure");
//		connection.setAuthenticated(false);
//	}
//
//	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
//	@Override
//	public void generate(JsonGenerator generator, String key) {
//		if (key!=null)
//			generator.writeStartObject(key);
//		else
//			generator.writeStartObject();
//		generator.write("reason", reason);
//		generator.writeEnd();
//	}
//	@Override                                    
//	public void parse(JsonParser parser) {       
//		String key = null;                         
//		boolean needsReason = true;
//		while (parser.hasNext()) {                 
//			JsonParser.Event e = parser.next();      
//			switch (e)                               
//			{                                        
//			case END_OBJECT:                         
//				if (needsReason)
//				{
//					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.key.KeyFailure\" needs \"reason\"");
//				}
//				return;                                
//			case KEY_NAME:                           
//				key = parser.getString();              
//				break;                                 
//			case VALUE_STRING:
//				if (key==null) { throw new RuntimeException("Value with no key!"); }
//				if (key.equals("reason")) {
//					needsReason = false;
//					reason = parser.getString();
//				} else {
//					LogWrapper.getLogger().warning("Unknown key: " + key);
//				}
//				break;
//			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
//			}
//		}
//	}
//	public static String getJsonName() { return "KeyFailure"; }
//	public String getJsonKey() { return getJsonName(); }
//	public KeyFailure(JsonParser parser) { parse(parser); }
//	public String toDebugString() {                                                      
//		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
//		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
//			generate(generator, null);                                                       
//		}                                                                                  
//		return new String(output.toByteArray());                                           
//	}                                                                                    
//	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
//}
