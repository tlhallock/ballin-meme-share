
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.CompressionStreams2;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class GetLogs extends Message
{
	byte[] decryptedNaunce;
	
	public GetLogs(byte[] decryptedNaunce)
	{
		this.decryptedNaunce = decryptedNaunce;
	}

	public String toString()
	{
		return "Give me your logs.";
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		byte[] param = (byte[]) connection.getParam("decryptedNaunce");
		if (!Arrays.equals(decryptedNaunce, param))
		{
			LogWrapper.getLogger().info("Update server machine failed authentication. Not serving logs.");
			connection.finish();
			return;
		}
		
		
		Path logFile = Services.settings.logFile.getPath();
		LogWrapper.getLogger().info("Serving logs at " + logFile.toAbsolutePath());
		long logSize = Files.size(logFile);
		long pushedSoFar = 0;
		byte[] buffer = new byte[Misc.BUFFER_SIZE];

		synchronized (connection.getOutput())
		{
			connection.send(new GotLogs(logSize));

			// Temporarily disable file logs...
			LogWrapper.logToFile(null, -1);

			try (OutputStream output = CompressionStreams2.newCompressedOutputStream(connection.getOutput());
					 InputStream input = Files.newInputStream(logFile);)
			{
				long rem = logSize - pushedSoFar;
				while (rem > 0)
				{
					LogWrapper.getLogger().fine("remaining: " + rem);
					int amountToRead = buffer.length;
					if (amountToRead > rem)
					{
						amountToRead = (int) rem;
					}
					int nread = input.read(buffer, 0, amountToRead);
					if (nread < 0)
					{
						LogWrapper.getLogger().info("Hit end of input from logs too early.");
						break;
					}
					output.write(buffer, 0, nread);
					pushedSoFar += nread;
					rem = Math.max(0, logSize - pushedSoFar);
				}
			}
			catch (IOException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to serve logs.", ex);
			}
			finally
			{
				// Then reopen it
				LogWrapper.logToFile(Services.settings.logFile.getPath(), Services.settings.logLength.get());
			}
			// finishWritingRemainingBytes(output, rem);
		}
	}

//	private static void finishWritingRemainingBytes(OutputStream output, long rem) throws IOException
//	{
//		int paddingLength = 50;
//		StringBuilder builder = new StringBuilder(paddingLength);
//		for (int i = 0; i < paddingLength; i++)
//		{
//			builder.append('\n');
//		}
//		byte[] bytes = builder.toString().getBytes();
//		while (rem > 0)
//		{
//			for (int i = 0; i < bytes.length && rem > 0; i++, rem--)
//			{
//				output.write(bytes[i]);
//			}
//		}
//	}


	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("decryptedNaunce", Misc.format(decryptedNaunce));
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsDecryptedNaunce = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsDecryptedNaunce)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.msg.swup.GetLogs\" needs \"decryptedNaunce\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("decryptedNaunce")) {
					needsDecryptedNaunce = false;
					decryptedNaunce = Misc.format(parser.getString());
				} else {
					LogWrapper.getLogger().warning(LogWrapper.getUnknownMessageAttributeStr(getJsonKey(), parser, e, key));
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "GetLogs"; }
	public String getJsonKey() { return getJsonName(); }
	public GetLogs(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	
//PublicKey remoteKey = connection.getAuthentication().getRemoteKey();
//PublicKey publicKey = Services.updateManager.getPublicKey();
//if (publicKey == null)
//{
//	LogWrapper.getLogger().info("Do not have update manager's public key. Unable to serve logs.");
//	connection.finish();
//	return;
//}
//
//if (!KeyPairObject.serialize(remoteKey).equals(KeyPairObject.serialize(publicKey)))
//{
//	LogWrapper.getLogger().info("Unable to serve logs: unable to keys did not match.");
//	return;
//}
}
