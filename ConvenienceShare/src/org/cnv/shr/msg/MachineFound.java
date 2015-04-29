package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

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
	
	public MachineFound(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
	}

	public MachineFound()
	{
		ip   = Services.settings.getLocalIp();
		port = Services.settings.defaultPort;
		keys = Services.keyManager.getKeys();
		name = Services.settings.machineName;
	}
	
	public MachineFound(Machine m)
	{
		ip   = m.getIp();
		port = m.getPort();
		keys = m.getKeys();
		name = m.getName();
	}
	
	@Override
	public void perform() throws Exception
	{
		Services.remotes.addMachine(new Machine(ip, port, keys));
	}

	@Override
	protected void parse(InputStream bytes) throws IOException
	{
		ip   =        ByteReader.readString(bytes);
		port =        ByteReader.readInt(bytes);
		name =        ByteReader.readString(bytes);
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
		buffer.append(keys.length);
		for (String key : keys)
		{
			buffer.append(key);
		}
	}
	
	protected int getType()
	{
		return TYPE;
	}
}
