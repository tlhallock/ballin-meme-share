package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class MachineFound extends Message
{
	private String ip;
	protected int port;
	protected int nports;
	protected String name;
	protected String ident;
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

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("ip", ip);
		generator.write("port", port);
		generator.write("nports", nports);
		generator.write("name", name);
		generator.write("ident", ident);
		generator.write("lastActive", lastActive);
		generator.writeEnd();
	}

	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "ip":
				ip = parser.getString();
				break;
			case "name":
				name = parser.getString();
				break;
			case "ident":
				ident = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "port":
				port = new BigDecimal(parser.getString()).intValue();
				break;
			case "nports":
				nports = new BigDecimal(parser.getString()).intValue();
				break;
			case "lastActive":
				lastActive = new BigDecimal(parser.getString()).longValue();
				break;
			}
			break;
			default: break;
			}
		}
	}
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
