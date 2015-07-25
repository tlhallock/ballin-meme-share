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
//import java.security.PublicKey;
//import java.util.Map.Entry;
//import java.util.Objects;
//
//import javax.json.stream.JsonGenerator;
//import javax.json.stream.JsonParser;
//
//import org.cnv.shr.cnctn.Communication;
//import org.cnv.shr.cnctn.IdkWhereToPutThis;
//import org.cnv.shr.dmn.Services;
//import org.cnv.shr.json.JsonMap;
//import org.cnv.shr.trck.TrackObjectUtils;
//import org.cnv.shr.util.AbstractByteWriter;
//import org.cnv.shr.util.ByteReader;
//import org.cnv.shr.util.LogWrapper;
//import org.cnv.shr.util.Misc;
//
//public class KeyNotFound extends KeyMessage
//{
//	private JsonMap tests = new JsonMap();
//
//	public KeyNotFound(InputStream stream) throws IOException
//	{
//		super(stream);
//	}
//	
//	public KeyNotFound(Communication c, PublicKey[] knownKeys) throws IOException
//	{
//		for (PublicKey publicKey : knownKeys)
//		{
//			Objects.requireNonNull(publicKey, "Known keys should not be null.");
//			tests.put(publicKey, IdkWhereToPutThis.createTestNaunce(c.getAuthentication(), publicKey));
//		}
//	}
//
//	@Override
//	protected void parse(ByteReader reader) throws IOException
//	{
//		int size = reader.readInt();
//		for (int i = 0; i < size; i++)
//		{
//			PublicKey readPublicKey = reader.readPublicKey();
//			byte[] readVarByteArray = reader.readVarByteArray();
//			System.out.println(tests);
//			tests.put(readPublicKey, readVarByteArray);
//		}
//	}
//
//	@Override
//	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
//	{
//		buffer.append(tests.size());
//		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
//		{
//			buffer.append(entry.getKey());
//			buffer.appendVarByteArray(entry.getValue());
//		}
//	}
//
//	public static int TYPE = 25;
//	@Override
//	protected int getType()
//	{
//		return TYPE;
//	}
//
//	@Override
//	public void perform(Communication connection) throws Exception
//	{
//		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
//		{
//			PublicKey knownKey = entry.getKey();
//			byte[] test = entry.getValue();
//			if (Services.keyManager.containsKey(knownKey))
//			{
//				// able to verify self to remote, but change key
//				byte[] decrypted = Services.keyManager.decrypt(knownKey, test);
//				// still need to authenticate them.
//				byte[] naunceRequest = IdkWhereToPutThis.createTestNaunce(connection.getAuthentication(), 
//						connection.getAuthentication().getRemoteKey());
//				connection.send(new KeyChange(knownKey, Services.keyManager.getPublicKey(), decrypted, naunceRequest));
//				return;
//			}
//		}
//		
//		PublicKey localKey = Services.keyManager.getPublicKey();
//		connection.getAuthentication().setLocalKey(localKey);
//		connection.send(new NewKey(localKey, 
//				IdkWhereToPutThis.createTestNaunce(connection.getAuthentication(), 
//						connection.getAuthentication().getRemoteKey())));
//	}
//
//	@Override
//	public String toString()
//	{
//		StringBuilder builder = new StringBuilder();
//		builder.append("Key not found. Known keys are: ");
//		for (Entry<PublicKey, byte[]> entry : tests.entrySet())
//		{
//			builder.append(Misc.format(entry.getKey().getEncoded()));
//			builder.append("->");
//			builder.append(Misc.format(entry.getValue()));
//			builder.append('\n');
//		}
//		return builder.toString();
//	}
//
//	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
//	@Override
//	public void generate(JsonGenerator generator, String key) {
//		if (key!=null)
//			generator.writeStartObject(key);
//		else
//			generator.writeStartObject();
//		{
//			generator.writeStartObject("tests");
//			tests.generate(generator);
//		}
//		generator.writeEnd();
//	}
//	@Override                                    
//	public void parse(JsonParser parser) {       
//		String key = null;                         
//		boolean needsTests = true;
//		while (parser.hasNext()) {                 
//			JsonParser.Event e = parser.next();      
//			switch (e)                               
//			{                                        
//			case END_OBJECT:                         
//				if (needsTests)
//				{
//					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.key.KeyNotFound\" needs \"tests\"");
//				}
//				return;                                
//			case KEY_NAME:                           
//				key = parser.getString();              
//				break;                                 
//			case START_OBJECT:
//				if (key==null) { throw new RuntimeException("Value with no key!"); }
//				if (key.equals("tests")) {
//					needsTests = false;
//					tests.parse(parser);
//				} else {
//					LogWrapper.getLogger().warning("Unknown key: " + key);
//				}
//				break;
//			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
//			}
//		}
//	}
//	public static String getJsonName() { return "KeyNotFound"; }
//	public String getJsonKey() { return getJsonName(); }
//	public KeyNotFound(JsonParser parser) { parse(parser); }
//	public String toDebugString() {                                                      
//		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
//		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
//			generate(generator, null);                                                       
//		}                                                                                  
//		return new String(output.toByteArray());                                           
//	}                                                                                    
//	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
//}
