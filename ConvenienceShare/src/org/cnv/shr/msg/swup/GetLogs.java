
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

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

public class GetLogs extends Message
{
	byte[] decryptedNaunce;
	
	public GetLogs(byte[] decryptedNaunce)
	{
		this.decryptedNaunce = decryptedNaunce;
	}
	
	@Override
	protected int getType() { return 0; }
	@Override
	protected void parse(ByteReader reader) throws IOException {}
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (!connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			LogWrapper.getLogger().info("Update server machine failed authentication. Not serving logs.");
			connection.finish();
			return;
		}
		
		Services.updateManager.checkForUpdates(null, true);
		
		LogWrapper.getLogger().info("Serving logs.");
		
		Path logFile = Services.settings.logFile.getPath();
		long logSize = Files.size(logFile);
		long pushedSoFar = 0;
		byte[] buffer = new byte[Misc.BUFFER_SIZE];
		
		synchronized (connection.getOutput())
		{
			connection.send(new GotLogs(logSize));

			// Temporarily disable file logs...
			LogWrapper.logToFile(null, -1);
			
			try (InputStream input = Files.newInputStream(logFile))
			{
				while (logSize - pushedSoFar > 0)
				{
					int amountToRead = buffer.length;
					if (amountToRead < logSize - pushedSoFar)
					{
						amountToRead = (int) (logSize - pushedSoFar);
					}
					int nread = input.read(buffer, 0, amountToRead);
					if (nread < 0)
					{
						break;
					}
					connection.getOutput().write(buffer, 0, nread);
					pushedSoFar += nread;
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
			
			for (long rem = logSize - pushedSoFar; rem > 0; rem--)
			{
				connection.getOutput().write((byte) '\n');
			}
		}
	}


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
		boolean needsdecryptedNaunce = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdecryptedNaunce)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs decryptedNaunce");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_STRING:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("decryptedNaunce")) {
					needsdecryptedNaunce = false;
					decryptedNaunce = Misc.format(parser.getString());
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
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
