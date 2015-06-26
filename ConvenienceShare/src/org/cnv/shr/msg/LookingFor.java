
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



package org.cnv.shr.msg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class LookingFor extends Message
{
	public static int TYPE = 28;
	
	private String checksum;
	private long fileSize;
	
	
	public LookingFor(FileEntry file)
	{
		checksum = file.getChecksum();
		fileSize = file.getFileSize();
	}

	public LookingFor(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer)
	{

	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		SharedFile file = DbFiles.getFile(checksum, fileSize);
		Machine machine = connection.getMachine();
		checkPermissionsViewable(connection, machine, "Reporting file");
		connection.send(new MachineHasFile(file));
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("checksum", checksum);
		generator.write("fileSize", fileSize);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsfileSize = true;
		boolean needschecksum = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsfileSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs fileSize");
				}
				if (needschecksum)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs checksum");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("fileSize")) {
				needsfileSize = false;
				fileSize = Long.parseLong(parser.getString());
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("checksum")) {
				needschecksum = false;
				checksum = parser.getString();
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "LookingFor"; }
	public String getJsonKey() { return getJsonName(); }
	public LookingFor(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
