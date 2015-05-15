package org.cnv.shr.cnctn;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.util.Misc;

public class Authenticator
{
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private Boolean authenticated;
	private HashSet<String> pendingNaunces = new HashSet<>();

	// The machine that the remote says it is
	private PublicKey remotePublicKey;
	private PublicKey localPublicKey;
	
	String claimedName;
	int claimedPort;
	int claimedNumPorts;

	private boolean acceptAnyKeys;
	
	public Authenticator(boolean acceptAnyKeys, PublicKey remote, PublicKey local)
	{
		this.acceptAnyKeys = acceptAnyKeys;
	}

	public Authenticator()
	{
		this.acceptAnyKeys = false;
	}
	
	public void setMachineInfo(String name, int port, int nports)
	{
		this.claimedName = name;
		this.claimedPort = port;
		this.claimedNumPorts = nports;
	}
	
	public void offerRemote(String id, String ip)
	{
		// If we have no record of this machine, then it can be authenticated. (We will not share with it anyway.)
		// Otherwise, it must prove that it who it said it was in case we are sharing with it.
		// Somebody could spam a whole bunch of these to make the database too big.
		// Should fix that sometime...
		if (id.equals(Services.localMachine.getIdentifier()))
		{
			return;
		}
		
		if (DbMachines.getMachine(id) != null)
		{
			return;
		}

		Services.logger.println("Found new machine!");
		updateMachineInfo(id, ip);
	}
	
	void updateMachineInfo(String ident, String ip)
	{
		if (ip == null)
		{
			new Exception().printStackTrace(System.out);
		}
		DbMachines.updateMachineInfo(ident, claimedName, new PublicKey[] { remotePublicKey }, ip, claimedPort, claimedNumPorts);
	}
	
	void notifyAuthentication(String id, String ip, boolean authenticated)
	{
		lock.lock();
		try
		{
			condition.signalAll();
			if (authenticated)
			{
				Services.logger.println("Remote is authenticated.");
				updateMachineInfo(id, ip);
				authenticated = true;
			}
			else
			{
				authenticated = false;
				Services.logger.println("Remote failed authentication.");
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	boolean waitForAuthentication() throws IOException
	{
		lock.lock();
		boolean returnValue = false;
		try
		{
			int count = 0;
			while (authenticated == null)
			{
				if (count++ > 60)
				{
					throw new IOException("Connection timed out...");
				}
				condition.await(10, TimeUnit.SECONDS);
			}
			returnValue = authenticated;
		}
		catch (InterruptedException e)
		{
			Services.logger.print(e);
		}
		finally
		{
			lock.unlock();
		}
		return returnValue;
	}

	
	public boolean authenticate(Message m)
	{
		// Double check identifier and keys...
		return !m.requiresAthentication() || (authenticated != null && authenticated);
	}

	public void setKeys(PublicKey remotePublicKey, PublicKey localPublicKey)
	{
		this.remotePublicKey = remotePublicKey;
		this.localPublicKey = localPublicKey;
	}
	public void setLocalKey(PublicKey localKey)
	{
		this.localPublicKey = localKey;
	}
	public void setRemoteKey(PublicKey publicKey)
	{
		this.remotePublicKey = publicKey;
	}
	
	public PublicKey getLocalKey()
	{
		return localPublicKey;
	}
	public PublicKey getRemoteKey()
	{
		return remotePublicKey;
	}

	public void addPendingNaunce(byte[] original)
	{
		pendingNaunces.add(Misc.format(original));
	}

	public boolean hasPendingNaunce(byte[] decrypted)
	{
		boolean returnValue = pendingNaunces.contains(Misc.format(decrypted));
		pendingNaunces.clear();
		return returnValue;
	}
	
	public void authenticateToTarget(Communication connection, byte[] requestedNaunce) throws IOException
	{
		PublicKey publicKey = Services.keyManager.getPublicKey();
		if (!Services.keyManager.containsKey(localPublicKey))
		{
			// not able to verify self to remote, add key
			localPublicKey = publicKey;
			final byte[] sentNaunce = Services.keyManager.createTestNaunce(this, remotePublicKey);
			connection.send(new NewKey(publicKey, sentNaunce));
			return;
		}
		
		byte[] decrypted = Services.keyManager.decryptNaunce(localPublicKey, requestedNaunce);
		byte[] naunceRequest = Services.keyManager.createTestNaunce(this, remotePublicKey);
		if (!Arrays.equals(publicKey.getEncoded(), localPublicKey.getEncoded()))
		{
			// able to verify self to remote, but change key
			connection.send(new KeyChange(localPublicKey, publicKey, decrypted, naunceRequest));
			localPublicKey = publicKey;
			return;
		}

		connection.send(new ConnectionOpenAwk(decrypted, naunceRequest));
	}

	public boolean acceptKey(PublicKey remote)
	{
		return acceptAnyKeys || Services.settings.acceptNewKeys.get();
	}

	public void assertCanSend(Message m)
	{
		if (m.requiresAthentication() && (authenticated == null || !authenticated))
		{
			throw new RuntimeException("Trying to send message on connection not yet authenticated.\n"
					+ "type = " + m.getClass().getName() + "\nmsg=\"" + m + "\"");
		}
	}
}
