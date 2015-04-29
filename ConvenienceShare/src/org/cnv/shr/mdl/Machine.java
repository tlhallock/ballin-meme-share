package org.cnv.shr.mdl;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.LinkedList;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Machine
{
	private String name;
	private String ip;
	private int port;
	private long lastActive;
	private boolean sharing;
	

	private LinkedList<PublicKey> publicKeys = new LinkedList<>();

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
	
	public Machine(String ip, int port, String[] keys)
	{
		this.ip = ip;
		this.port = port;
		for (String key : keys)
		{
			
		}
	}
	
	public Machine(JSONObject object) throws JSONException
	{
		ip = object.getString("ip");
		port = object.getInt("port");
		
		JSONArray arr = object.getJSONArray("keys");
		for (int i = 0; i < arr.length(); i++)
		{
			String key = arr.getString(i);
		}
		lastActive = object.getLong("lastActive");
	}
	
	public void append(JSONArray machines) throws JSONException
	{
		LinkedList<String> keys = new LinkedList<>();
		for (PublicKey key : publicKeys)
		{
			keys.add(Misc.format(key.getEncoded()));
		}
		JSONObject object = new JSONObject();
		object.put("ip", ip);
		object.put("port", port);
		object.put("keys", new JSONArray(keys));
		object.put("lastActive", lastActive);
		
		machines.put(machines.length(), object);
	}


	public String getName()
	{
		return "Foobar";
	}
	
	public String getIp()
	{
		return ip;
	}
	
	public int getPort()
	{
		return port;
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
		return ip + ":" + port + "[" + publicKeys + "]";
	}

	public int hashCode()
	{
		return toString().hashCode();
	}

	public boolean equals(Object other)
	{
		return other instanceof Machine && toString().equals(other.toString());
	}

	public String[] getKeys()
	{
		return new String[0];
	}

	public void setSharing(boolean b)
	{
		sharing = b;
	}

	public void setName(String string)
	{
		name = string;
	}

	public void setLastActive(long long1)
	{
		lastActive = long1;
	}
}
