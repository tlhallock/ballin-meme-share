package org.cnv.shr.mdl;

import java.io.IOException;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.ListRoots;
import org.cnv.shr.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Machine extends DbObject
{
	private String ip;
	private int port;
	
	private String name;
	private String identifier = "";
	
	private long lastActive;
	private Boolean sharing;
	
	private int nports;
	private boolean allowsMessages;
	
	private LinkedList<PublicKey> publicKeys = new LinkedList<>();

	/*
	public Machine(String machine)
	{
		int index = machine.indexOf(':');
		if (index < 0)
		{
			this.ip = machine;
			port = Services.settings.defaultPort;
		}
		else
		{
			ip = machine.substring(0, index);
			port = Integer.parseInt(machine.substring(index + 1, machine.length()));
		}
	}
	*/
	
	public Machine(
			String ip, int port, int nports,
			String name, String identifier,
			String[] keys)
	{
		super(null);
		this.ip = ip;
		this.port = port;
		this.name = name;
		this.identifier = identifier;
		this.nports = nports;
		this.sharing = false;
		for (String key : keys)
		{
			
		}
	}
	
	protected Machine() { super(null); }

	public Machine(int int1)
	{
		super(int1);
	}

	@Override
	public void fill(java.sql.Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
			id             = row.getInt   ("M_ID");        
			name           = row.getString("MNAME");        
			ip             = row.getString("IP");    
			port		   = row.getInt   ("PORT");
			nports         = row.getInt   ("NPORTS");
		    lastActive     = row.getLong  ("LAST_ACTIVE");
		    sharing        = row.getBoolean("SHARING");
		    identifier     = row.getString ("IDENT");
		    allowsMessages = row.getBoolean("MESSAGES");
	}
	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
		PreparedStatement stmt = c.prepareStatement(
				 "merge into MACHINE key(IDENT) values ((select M_ID from MACHINE where MACHINE.IDENT = ?), ?, ?, ?, ?, ?, ?, ?, ?, ?);"
				, Statement.RETURN_GENERATED_KEYS);
		int ndx = 1;
		stmt.setString(ndx++,  getIdentifier());
		stmt.setString(ndx++,  getName());
		stmt.setString(ndx++,  getIp());
		stmt.setInt(ndx++,     getPort());
		stmt.setInt(ndx++,     getNumberOfPorts());
		stmt.setLong(ndx++,    System.currentTimeMillis());
		stmt.setBoolean(ndx++, isSharing());
		stmt.setString(ndx++,  getIdentifier());
		stmt.setBoolean(ndx++, isLocal());
		stmt.setBoolean(ndx++, getAllowsMessages());
		return stmt;
	}
	
	public int getNumberOfPorts()
	{
		return nports;
	}
	
	public boolean getAllowsMessages()
	{
		return allowsMessages;
	}


	public void setIp(String string)
	{
		this.ip = string;
	}

	public void setPort(int int1)
	{
		this.port = int1;
	}

	public void setIdentifier(String string)
	{
		this.identifier = string;
	}

	public String getIdentifier()
	{
		return identifier;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getIp()
	{
		return ip;
	}
	
	public int getPort()
	{
		return port;
	}

	public String toString()
	{
		return getIp() + ":" + getPort() + "[id=" + getIdentifier() + "][keys=" + getKeys() + "]";
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
	
	public long getLastActive()
	{
		return lastActive;
	}

	public boolean isSharing()
	{
		return sharing;
	}

	public boolean isLocal()
	{
		return false;
	}

	public void setNumberOfPorts(int nports2)
	{
		nports = nports2;
	}

	public void refresh()
	{
		try
		{
			Communication openConnection = Services.networkManager.openConnection(ip, port);
			openConnection.send(new FindMachines());
			openConnection.send(new ListRoots());
			openConnection.notifyDone();
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to discover refresh " + this);
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public String getUrl()
	{
		return getIp() + ":" + getPort();
	}
	
	
	/**
	 * 
	
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
	
	 */
	

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
	
	public static class LocalMachine extends Machine
	{
		public LocalMachine()
		{
			Services.keyManager.getKeys();
		}
		
		public void setLastActive(long long1)
		{
			throw new UnsupportedOperationException("This is the local machine.");
		}
		
		public void setSharing(boolean b)
		{
			throw new UnsupportedOperationException("This is the local machine.");
		}

		public void setName(String string)
		{
			throw new UnsupportedOperationException("This is the local machine.");
		}

		public long getLastActive()
		{
			return System.currentTimeMillis();
		}
		
		public boolean isSharing()
		{
			return true;
		}

		public boolean isLocal()
		{
			return true;
		}

		public String getName()
		{
			return Services.settings.machineName.get();
		}
		
		public String getIp()
		{
			return Services.settings.getLocalIp();
		}
		
		public int getPort()
		{
			return Services.settings.servePortBegin.get();
		}

		public String[] getKeys()
		{
			return new String[0];
		}
		
		public String getIdentifier()
		{
			return Services.settings.machineIdentifier.get();
		}
		
		public int getNumberOfPorts()
		{
			return Services.settings.maxServes.get();
		}
		
		public boolean getAllowsMessages()
		{
			return true;
		}

		public void setId()
		{
			id = DbMachines.getMachineId(getIdentifier());
		}
	}
}
