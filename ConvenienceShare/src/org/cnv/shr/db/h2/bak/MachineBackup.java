
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
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.json.JsonStringList;
import org.cnv.shr.json.JsonStringMap;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

public class MachineBackup implements Jsonable
{
	private String ip;
	private int port;
	private int nports;
	private String name;
	private String identifier;
	private boolean allowsMessages;
	private SharingState weShareToThem;
	private SharingState sharesWithUs;
	private boolean pin;
	private JsonStringList keys = new JsonStringList();
	private JsonStringMap roots = new JsonStringMap();
	
	
	public MachineBackup(Machine machine)
	{
		this.ip = machine.getIp();
		this.port = machine.getPort();
		this.nports = machine.getNumberOfPorts();
		this.name = machine.getName();
		this.identifier = machine.getIdentifier();
		this.allowsMessages = machine.getAllowsMessages();
		this.weShareToThem = machine.sharingWithOther();
		this.sharesWithUs = machine.getSharesWithUs();
		
		for (PublicKey publicKey : DbKeys.getKeys(machine))
		{
			keys.add(KeyPairObject.serialize(publicKey));
		}
		
		try (DbIterator<RootDirectory> iterator = DbRoots.list(machine))
		{
			while (iterator.hasNext())
			{
				RootDirectory next = iterator.next();
				String fullPath = next.getPathElement().getFullPath();
				if (!fullPath.endsWith("/"))
				{
					fullPath = fullPath + "/";
				}
				roots.put(next.getName(), fullPath);
			}
		}
	}
	
	public void save(ConnectionWrapper wrapper)
	{
		Machine machine = new Machine(
				ip,
				port,
				nports,
				name,
				identifier,
				allowsMessages,
				weShareToThem,
				sharesWithUs,
				pin);
		
		try
		{
			machine.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to restore machine: " + identifier, e);
			return;
		}
		
		for (String key : keys)
		{
			DbKeys.addKey(machine, KeyPairObject.deSerializePublicKey(key));
		}
		
		for (Entry<String, String> entry : roots.entrySet())
		{
			String dirName = entry.getKey();
			String path = entry.getValue();
			RemoteDirectory root = (RemoteDirectory) DbRoots.getRoot(machine, dirName);
			if (root == null)
			{
				root = new RemoteDirectory(machine, dirName, null, null, SharingState.DO_NOT_SHARE);
				try
				{
					root.save(wrapper);
				}
				catch (SQLException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to save root directory: " + dirName, e);
					continue;
				}
			}
			PathElement pathElement = DbPaths.getPathElement(path);
			root.setLocalMirror(pathElement);
			DbPaths.pathLiesIn(pathElement, root);
		}
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
		generator.write("nports", nports);
		generator.write("name", name);
		generator.write("identifier", identifier);
		generator.write("allowsMessages", allowsMessages);
		generator.write("weShareToThem",weShareToThem.name());
		generator.write("sharesWithUs",sharesWithUs.name());
		generator.write("pin", pin);
		{
			generator.writeStartArray("keys");
			keys.generate(generator);
		}
		{
			generator.writeStartObject("roots");
			roots.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsallowsMessages = true;
		boolean needspin = true;
		boolean needsroots = true;
		boolean needsport = true;
		boolean needsnports = true;
		boolean needskeys = true;
		boolean needsip = true;
		boolean needsname = true;
		boolean needsidentifier = true;
		boolean needsweShareToThem = true;
		boolean needssharesWithUs = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsallowsMessages)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs allowsMessages");
				}
				if (needspin)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pin");
				}
				if (needsallowsMessages)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs allowsMessages");
				}
				if (needspin)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs pin");
				}
				if (needsroots)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs roots");
				}
				if (needsport)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs port");
				}
				if (needsnports)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs nports");
				}
				if (needskeys)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs keys");
				}
				if (needsip)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs ip");
				}
				if (needsname)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs name");
				}
				if (needsidentifier)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs identifier");
				}
				if (needsweShareToThem)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs weShareToThem");
				}
				if (needssharesWithUs)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs sharesWithUs");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_FALSE:
			if (key==null) break;
			switch(key) {
			case "allowsMessages":
				needsallowsMessages = false;
				allowsMessages = false;
				break;
			case "pin":
				needspin = false;
				pin = false;
				break;
			}
			break;
		case VALUE_TRUE:
			if (key==null) break;
			switch(key) {
			case "allowsMessages":
				needsallowsMessages = false;
				allowsMessages = true;
				break;
			case "pin":
				needspin = false;
				pin = true;
				break;
			}
			break;
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("roots")) {
				needsroots = false;
				roots.parse(parser);
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "port":
				needsport = false;
				port = Integer.parseInt(parser.getString());
				break;
			case "nports":
				needsnports = false;
				nports = Integer.parseInt(parser.getString());
				break;
			}
			break;
		case START_ARRAY:
			if (key==null) break;
			if (key.equals("keys")) {
				needskeys = false;
				keys.parse(parser);
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ip":
				needsip = false;
				ip = parser.getString();
				break;
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "identifier":
				needsidentifier = false;
				identifier = parser.getString();
				break;
			case "weShareToThem":
				needsweShareToThem = false;
				weShareToThem = SharingState.valueOf(parser.getString());
				break;
			case "sharesWithUs":
				needssharesWithUs = false;
				sharesWithUs = SharingState.valueOf(parser.getString());
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "MachineBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public MachineBackup(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
