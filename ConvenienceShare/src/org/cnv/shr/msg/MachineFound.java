package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class MachineFound extends Message
{
	@MyParserIgnore
	private String ip;
	protected int port;
	protected int nports;
	protected String name;
	protected String ident;
	@MyParserIgnore
	private long lastActive;
	
	public MachineFound(InputStream stream) throws IOException
	{
		super(stream);
	}

	public MachineFound()
	{
		this(Services.localMachine);
	}
	
	public MachineFound(Machine m)
	{
		ip         = m.getIp();
		port       = m.getPort();
		name       = m.getName();
		ident      = m.getIdentifier();
		lastActive = m.getLastActive();
		nports     = m.getNumberOfPorts();
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (ident.equals(Services.localMachine.getIdentifier()))
		{
			return;
		}
		DbMachines.updateMachineInfo(
				ident,
				name,
				null,
				ip,
				port,
				nports);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		ip          = reader.readString();
		port        = reader.readInt();
		name        = reader.readString();
		ident       = reader.readString();
		lastActive  = reader.readLong();
		nports      = reader.readInt();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(name);
		buffer.append(ident);
		buffer.append(lastActive);
		buffer.append(nports);
	}

	public static int TYPE = 18;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("There is a machine with ident=" + ident + " at " + ip + ":" + port);
		
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("port", port);
		generator.write("nports", nports);
		if (name!=null)
		generator.write("name", name);
		if (ident!=null)
		generator.write("ident", ident);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsname = true;
		boolean needsident = true;
		boolean needsport = true;
		boolean needsnports = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needsident)
				{
					throw new RuntimeException("Message needs ident");
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
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "ident":
				needsident = false;
				ident = parser.getString();
				break;
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
	public static String getJsonName() { return "MachineFound"; }
	public MachineFound(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
