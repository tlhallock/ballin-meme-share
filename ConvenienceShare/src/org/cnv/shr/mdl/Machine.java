package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.dmn.Services;

public class Machine extends DbObject<Integer>
{
	private String ip;
	private int port;
	
	private String name;
	private String identifier;
	
	private long lastActive;
	private Boolean sharing;
	
	private int nports;
	private boolean allowsMessages;
	
	protected boolean acceptPeers;
	
	
	public Machine(String identifier)
	{
		super(null);
		this.identifier = identifier;
		sharing = false;
	}
	
	protected Machine() { super(null); }

	public Machine(int int1)
	{
		super(int1);
	}

	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
			id             = row.getInt    ("M_ID");        
			name           = row.getString ("MNAME");        
			ip             = row.getString ("IP");    
			port		   = row.getInt    ("PORT");
			nports         = row.getInt    ("NPORTS");
		    lastActive     = row.getLong   ("LAST_ACTIVE");
		    sharing        = row.getBoolean("SHARING");
		    identifier     = row.getString ("IDENT");
		    allowsMessages = row.getBoolean("MESSAGES");
		    acceptPeers    = row.getBoolean("ACCEPT_PEERS");
	}

	@Override
	public boolean save(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				 "merge into MACHINE key(IDENT) values "
				 + "((select M_ID from MACHINE where IDENT=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
				, Statement.RETURN_GENERATED_KEYS);)
		{
			int ndx = 1;
			stmt.setString( ndx++, getIdentifier());
			writeMachine(stmt, ndx);
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				id = generatedKeys.getInt(1);
			}
		}
		if (id == null)
		{
			id = DbMachines.getMachineId(getIdentifier());
		}
		return true;
	}

	protected void writeMachine(PreparedStatement stmt, int ndx) throws SQLException
	{
		stmt.setString( ndx++, getName());
		stmt.setString( ndx++, getIp());
		stmt.setInt(    ndx++, getPort());
		stmt.setInt(    ndx++, getNumberOfPorts());
		stmt.setLong(   ndx++, System.currentTimeMillis());
		stmt.setBoolean(ndx++, isSharing());
		stmt.setString( ndx++, getIdentifier());
		stmt.setBoolean(ndx++, isLocal());
		stmt.setBoolean(ndx++, getAllowsMessages());
		stmt.setBoolean(ndx++, getAcceptPeers());
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
		return getIp() + ":" + getPort() + "[id=" + getIdentifier() + "]";
	}

	public int hashCode()
	{
		return toString().hashCode();
	}

	public boolean equals(Object other)
	{
		return other instanceof Machine && toString().equals(other.toString());
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
	
	public boolean getAcceptPeers()
	{
		return acceptPeers;
	}

	public boolean isLocal()
	{
		return false;
	}

	public void setNumberOfPorts(int nports2)
	{
		nports = nports2;
	}

	public String getUrl()
	{
		return getIp() + ":" + getPort();
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
		@Override
		public boolean save(Connection c) throws SQLException
		{
			try (PreparedStatement stmt = c.prepareStatement(
					 "merge into MACHINE key(IS_LOCAL) values "
					 + "((select M_ID from MACHINE where IS_LOCAL=true), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
					, Statement.RETURN_GENERATED_KEYS);)
			{
				int ndx = 1;
				writeMachine(stmt, ndx);
				stmt.executeUpdate();
				ResultSet generatedKeys = stmt.getGeneratedKeys();
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
				}
			}
			if (id == null)
			{
				id = DbMachines.getMachineId(getIdentifier());
			}
			return true;
		}
	}
}
