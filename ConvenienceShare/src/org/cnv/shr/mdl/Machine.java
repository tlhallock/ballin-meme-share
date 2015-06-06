package org.cnv.shr.mdl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;

public class Machine extends DbObject<Integer>
{
	private static final QueryWrapper MEREGE1 = new QueryWrapper("merge into MACHINE key(IDENT) values ((select M_ID from MACHINE where IDENT=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
	private static final QueryWrapper MERGE2  = new QueryWrapper("merge into MACHINE key(IS_LOCAL) values ((select M_ID from MACHINE where IS_LOCAL=true), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
	private String ip;
	private int port;
	
	private String name;
	private String identifier;
	
	private long lastActive;
	
	private int nports;
	private boolean allowsMessages;
	
	protected boolean acceptPeers;
	
	private SharingState weShareToThem;
	private SharingState sharesWithUs;
	
	public Machine(String identifier)
	{
		super(null);
		this.identifier = identifier;
		weShareToThem = SharingState.valueOf(Services.settings.defaultPermission.get());
	}

	public Machine(String ip2, int port2, int nports2, String name2, String identifier2, boolean allowsMessages2, SharingState weShareToThem2, SharingState sharesWithUs2)
	{
		super(null);
		
		this.ip = ip2;
		this.port = port2;
		this.nports = nports2;
		this.name = name2;
		this.identifier = identifier2;
		this.allowsMessages = allowsMessages2;
		this.weShareToThem = weShareToThem2;
		this.sharesWithUs = sharesWithUs2;
	}
	
	protected Machine() { super(null); }

	public Machine(int int1)
	{
		super(int1);
	}

	@Override
	public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
	{
			id             = row.getInt    ("M_ID");        
			name           = row.getString ("MNAME");        
			ip             = row.getString ("IP");    
			port		       = row.getInt    ("PORT");
			nports         = row.getInt    ("NPORTS");
		  lastActive     = row.getLong   ("LAST_ACTIVE");
		  weShareToThem  = SharingState.get(row.getInt("SHARING"));
		  sharesWithUs   = SharingState.get(row.getInt("SHARES_WITH_US"));
		  identifier     = row.getString ("IDENT");
		  allowsMessages = row.getBoolean("MESSAGES");
		  acceptPeers    = row.getBoolean("ACCEPT_PEERS");
	}

	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		try (StatementWrapper stmt = c.prepareStatement(MEREGE1, Statement.RETURN_GENERATED_KEYS);)
		{
			int ndx = 1;
			stmt.setString( ndx++, getIdentifier());
			writeMachine(stmt, ndx);
			stmt.executeUpdate();
			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
				}
			}
		}
		if (id == null)
		{
			id = DbMachines.getMachineId(getIdentifier());
		}
		return true;
	}

	protected void writeMachine(StatementWrapper stmt, int ndx) throws SQLException
	{
		stmt.setString( ndx++, getName());
		stmt.setString( ndx++, getIp());
		stmt.setInt(    ndx++, getPort());
		stmt.setInt(    ndx++, getNumberOfPorts());
		stmt.setLong(   ndx++, System.currentTimeMillis());
		stmt.setInt(    ndx++, sharingWithOther().getDbValue());
		stmt.setInt(    ndx++, getSharesWithUs().getDbValue());
		stmt.setString( ndx++, getIdentifier());
		stmt.setBoolean(ndx++, isLocal());
		stmt.setBoolean(ndx++, getAllowsMessages());
		stmt.setBoolean(ndx++, getAcceptPeers());
	}

	public int getNumberOfPorts()
	{
		return nports;
	}

	public void setAllowsMessages(boolean b)
	{
		this.allowsMessages = b;
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

	@Override
	public String toString()
	{
		return getIp() + ":" + getPort() + "[id=" + getIdentifier() + "]";
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof Machine && toString().equals(other.toString());
	}
	
	public void setTheyShare(SharingState state)
	{
		this.sharesWithUs = state;
	}

	public void setWeShare(SharingState state)
	{
		weShareToThem = state;
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

	public SharingState sharingWithOther()
	{
		return weShareToThem;
	}
	
	public SharingState getSharesWithUs()
	{
		if (sharesWithUs == null)
		{
			return SharingState.DO_NOT_SHARE;
		}
		return sharesWithUs;
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
		public LocalMachine() {}

		@Override
		public String toString()
		{
			return "Local machine: " + super.toString();
		}
		
		@Override
		public void setLastActive(long long1)
		{
			throw new UnsupportedOperationException("This is the local machine.");
		}
		
		@Override
		public void setWeShare(SharingState state)
		{
			throw new UnsupportedOperationException("This is the local machine.");
		}

		@Override
		public void setName(String string)
		{
			throw new UnsupportedOperationException("This is the local machine.");
		}

		@Override
		public long getLastActive()
		{
			return System.currentTimeMillis();
		}
		
		@Override
		public SharingState sharingWithOther()
		{
			return SharingState.DOWNLOADABLE;
		}

		@Override
		public SharingState getSharesWithUs()
		{
			return SharingState.DOWNLOADABLE;
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}

		@Override
		public String getName()
		{
			return Services.settings.machineName.get();
		}
		
		@Override
		public String getIp()
		{
			return Services.settings.getLocalIp();
		}
		
		@Override
		public int getPort()
		{
			return Services.settings.servePortBeginE.get();
		}
		
		@Override
		public String getIdentifier()
		{
			return Services.settings.machineIdentifier.get();
		}
		
		@Override
		public int getNumberOfPorts()
		{
			return Services.settings.maxServes.get();
		}
		
		@Override
		public boolean getAllowsMessages()
		{
			return true;
		}

		public void setId()
		{
			id = DbMachines.getMachineId(getIdentifier());
		}
		@Override
		public boolean save(ConnectionWrapper c) throws SQLException
		{
			super.save(c);
			if (id == null)
			{
				id = DbMachines.getMachineId(getIdentifier());
			}
			return true;
		}
	}
}
