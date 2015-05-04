package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class MachineFound extends Message
{
	public static int TYPE = 1;
	
	private String ip;
	private int port;
	private String[] keys;
	private String name;
	private String ident;
	private long lastActive;
	
	public MachineFound(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}

	public MachineFound()
	{
		this(Services.localMachine);
	}
	
	public MachineFound(Machine m)
	{
		ip         = m.getIp();
		port       = m.getPort();
		keys       = m.getKeys();
		name       = m.getName();
		ident      = m.getIdentifier();
		lastActive = m.getLastActive();
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine newMachine = new Machine(ip, port, name, ident, keys);
		newMachine.setLastActive(lastActive);
		Services.remotes.addMachine(newMachine);
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		ip          = ByteReader.readString(bytes);
		port        = ByteReader.readInt(bytes);
		name        = ByteReader.readString(bytes);
		ident       = ByteReader.readString(bytes);
		lastActive  = ByteReader.readLong(bytes);
		int numKeys = ByteReader.readInt(bytes);
		keys = new String[numKeys];
		for (int i = 0; i < numKeys; i++)
		{
			keys[i] = ByteReader.readString(bytes);
		}
	}

	@Override
	protected void write(ByteListBuffer buffer)
	{
		buffer.append(ip);
		buffer.append(port);
		buffer.append(name);
		buffer.append(ident);
		buffer.append(lastActive);
		buffer.append(keys.length);
		for (String key : keys)
		{
			buffer.append(key);
		}
	}

	public boolean authenticate()
	{
		return true;
	}
	
	protected int getType()
	{
		return TYPE;
	}
}
