package org.cnv.shr.mdl;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.cnv.shr.dmn.Settings;

public class Machine
{
	String ip;
	int port = Settings.getInstance().getDefaultPort();
	String user;

	public Machine(String machine)
	{
		int index = machine.indexOf(':');
		if (index < 0)
		{
			this.ip = machine;
		}
		else
		{
			ip = machine.substring(0, index);
			port = Integer.parseInt(machine.substring(index + 1, machine.length()));
		}
	}

	public Machine(String ip, int port)
	{
		this.ip = ip;
		this.port = port;
	}

	public Socket open() throws UnknownHostException, IOException
	{
		return new Socket(ip, port);
	}

	public String toString()
	{
		return ip + ":" + port;
	}

	public int hashCode()
	{
		return toString().hashCode();
	}

	public boolean equals(Object other)
	{
		return other instanceof Machine && toString().equals(other.toString());
	}
}
