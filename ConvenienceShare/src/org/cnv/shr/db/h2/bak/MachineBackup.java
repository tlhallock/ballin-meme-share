package org.cnv.shr.db.h2.bak;

import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.Jsonable;
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
				sharesWithUs);
		
		try
		{
			machine.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to restore machine: " + identifier, e);
		}
	}
	
	

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.writeStartObject();
		if (ip!=null)
		generator.write("ip", ip);
		generator.write("port", port);
		generator.write("nports", nports);
		if (name!=null)
		generator.write("name", name);
		if (identifier!=null)
		generator.write("identifier", identifier);
		generator.write("allowsMessages", allowsMessages);
		if (weShareToThem!=null)
		generator.write("weShareToThem",weShareToThem.name());
		if (sharesWithUs!=null)
		generator.write("sharesWithUs",sharesWithUs.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsip = true;
		boolean needsname = true;
		boolean needsidentifier = true;
		boolean needsweShareToThem = true;
		boolean needssharesWithUs = true;
		boolean needsallowsMessages = true;
		boolean needsport = true;
		boolean needsnports = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsip)
				{
					throw new RuntimeException("Message needs ip");
				}
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needsidentifier)
				{
					throw new RuntimeException("Message needs identifier");
				}
				if (needsweShareToThem)
				{
					throw new RuntimeException("Message needs weShareToThem");
				}
				if (needssharesWithUs)
				{
					throw new RuntimeException("Message needs sharesWithUs");
				}
				if (needsallowsMessages)
				{
					throw new RuntimeException("Message needs allowsMessages");
				}
				if (needsallowsMessages)
				{
					throw new RuntimeException("Message needs allowsMessages");
				}
				if (needsport)
				{
					throw new RuntimeException("Message needs port");
				}
				if (needsnports)
				{
					throw new RuntimeException("Message needs nports");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
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
				weShareToThem = SharingState.valueOf(parser.getString());;
				break;
			case "sharesWithUs":
				needssharesWithUs = false;
				sharesWithUs = SharingState.valueOf(parser.getString());;
				break;
			}
			break;
		case VALUE_FALSE:
			if (key==null) break;
			if (key.equals("allowsMessages")) {
				needsallowsMessages = false;
				allowsMessages = false;
			}
			break;
		case VALUE_TRUE:
			if (key==null) break;
			if (key.equals("allowsMessages")) {
				needsallowsMessages = false;
				allowsMessages = true;
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
			default: break;
			}
		}
	}
	public static String getJsonName() { return "MachineBackup"; }
	public MachineBackup(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
