package org.cnv.shr.cnctn;

import javax.swing.JFrame;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.Misc;

public abstract class ConnectionParams
{
	JFrame origin;
	String identifier;
	String ip;
	int port;
	boolean acceptAllKeys;
	String reason;
	
	private boolean notified;

	public ConnectionParams(String url, boolean acceptKeys, String reason)
	{
		this.origin = null;
		this.identifier = null;
		this.ip = ConnectionManager.getIp(url);
		this.port = ConnectionManager.getPort(url);
		this.acceptAllKeys = acceptKeys;
		this.reason = reason;
	}
	public ConnectionParams(JFrame origin, Machine m, boolean acceptKeys, String reason)
	{
		this.origin = origin;
		setMachine(m);
		this.acceptAllKeys = acceptKeys;
		this.reason = reason;
	}
	protected ConnectionParams() {}
	
	public ConnectionParams setMachine(Machine machine)
	{
		identifier = machine.getIdentifier();
		ip = machine.getIp();
		port = machine.getPort();
		return this;
	}

	protected abstract boolean closeWhenDone();
	
	protected abstract void opened(Communication connection) throws Exception;
	protected void failed() {}
	
	protected void notifyOpened(Communication connection) throws Exception
	{
		notified = true;
		opened(connection);
	}
	protected void notifyFailed()
	{
		notified = true;
		failed();
	}
	
	boolean tryingToConnectToLocal()
	{
		return Misc.collectIps().contains(ip) && port == Services.localMachine.getPort();
//			&&  (portBegin            >= Services.localMachine.getPort() && portBegin            <= Services.localMachine.getPort() + Services.localMachine.getNumberOfPorts())
//					||
//					(portBegin + numPorts >= Services.localMachine.getPort() && portBegin + numPorts <= Services.localMachine.getPort() + Services.localMachine.getNumberOfPorts());
	}
	
	public static abstract class AutoCloseConnectionParams extends ConnectionParams
	{
		public AutoCloseConnectionParams(String url, boolean acceptKeys, String reason)
		{
			super(url, acceptKeys, reason);
		}
		public AutoCloseConnectionParams(JFrame origin, Machine m, boolean acceptKeys, String reason)
		{
			super(origin, m, acceptKeys, reason);
		}
		public final boolean closeWhenDone() { return true; }
	}
	public static abstract class KeepOpenConnectionParams extends ConnectionParams
	{
		public KeepOpenConnectionParams(String url, boolean acceptKeys, String reason)
		{
			super(url, acceptKeys, reason);
		}
		public KeepOpenConnectionParams(JFrame origin, Machine m, boolean acceptKeys, String reason)
		{
			super(origin, m, acceptKeys, reason);
		}
		public final boolean closeWhenDone() { return false; }
	}
	
	public void ensureNotification()
	{
		if (!notified)
		{
			notifyFailed();
		}
	}
}