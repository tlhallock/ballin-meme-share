package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class MachineFound extends Message
{
	private String ip;
	private int port;
	private int nports;
	private String name;
	private String ident;
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
		DbMachines.updateMachineInfo(createMachine(), null, connection.getIp());
	}

	public Machine createMachine()
	{
		Machine newMachine = new Machine(ip, port, nports, name, ident);
		newMachine.setLastActive(lastActive);
		return newMachine;
	}

	@Override
	public void parse(InputStream bytes) throws IOException
	{
		ip          = ByteReader.readString(bytes);
		port        = ByteReader.readInt(bytes);
		name        = ByteReader.readString(bytes);
		ident       = ByteReader.readString(bytes);
		lastActive  = ByteReader.readLong(bytes);
		nports      = ByteReader.readInt(bytes);
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(name);
		buffer.append(ident);
		buffer.append(lastActive);
		buffer.append(nports);
	}

	public static int TYPE = 18;
	protected int getType()
	{
		return TYPE;
	}

	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("There is a machine with ident=" + ident + " at " + ip + ":" + port);
		
		return builder.toString();
	}
}
