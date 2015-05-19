package org.cnv.shr.cnctn;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AcceptKey;
import org.cnv.shr.mdl.Machine;
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
	
	private String claimedName;
	private int claimedPort;
	private int claimedNumPorts;

	private boolean acceptAnyKeys;
	
	public Authenticator(boolean acceptAnyKeys, PublicKey remote, PublicKey local)
	{
		this.acceptAnyKeys = acceptAnyKeys;
		this.remotePublicKey = remote;
		this.localPublicKey = local;
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
	
	public void offerRemote(String id, String ip, PublicKey[] keys)
	{
		// If we have no record of this machine, then it can be authenticated. (We will not share with it anyway.)
		// Otherwise, it must prove that it who it said it was in case we are sharing with it.
		// Somebody could spam a whole bunch of these to make the database too big.
		// Should fix that sometime...
		if (id.equals(Services.localMachine.getIdentifier()))
		{
			return;
		}

		if (remotePublicKey == null && keys.length > 0)
		{
			remotePublicKey = keys[0];
		}

		if (DbMachines.getMachine(id) != null)
		{
			return;
		}
		Services.logger.println("Found new machine!");
		updateMachineInfo(id, ip, keys);
	}
	
	void updateMachineInfo(String ident, String ip, PublicKey[] keys)
	{
		DbMachines.updateMachineInfo(ident, claimedName, keys, ip, claimedPort, claimedNumPorts);
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

				updateMachineInfo(id, ip, new PublicKey[] { remotePublicKey });
				this.authenticated = true;
			}
			else
			{
				this.authenticated = false;
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
	
	public boolean canAuthenticateRemote(Communication connection, PublicKey remote, PublicKey local) throws IOException
	{
		this.localPublicKey = local;
		this.remotePublicKey = remote;
		
		Machine machine = connection.getMachine();
		
		// authenticate remote...
		if (DbKeys.machineHasKey(machine, remote))
		{
			Services.logger.println("We have a the key for the remote.");
			return true;
		}

		if (acceptKey(connection, remote, machine))
		{
			Services.logger.println("We have accepted the key for the remote.");
			DbKeys.addKey(machine, remote);
			return true;
		}

		Services.logger.println("Unable to accept remote key.");
		// add message
		return false;
	}
	
	public void authenticateToTarget(Communication connection, byte[] requestedNaunce) throws IOException
	{
		PublicKey publicKey = Services.keyManager.getPublicKey();
		if (!Services.keyManager.containsKey(localPublicKey))
		{
			Services.logger.println("The remote's key for us will not do.");
			// not able to verify self to remote, add key
			localPublicKey = publicKey;
			final byte[] sentNaunce = Services.keyManager.createTestNaunce(this, remotePublicKey);
			connection.send(new NewKey(publicKey, sentNaunce));
			return;
		}
		
		byte[] decrypted = Services.keyManager.decrypt(localPublicKey, requestedNaunce);
		byte[] naunceRequest = Services.keyManager.createTestNaunce(this, remotePublicKey);
		if (!Arrays.equals(publicKey.getEncoded(), localPublicKey.getEncoded()))
		{
			Services.logger.println("We have the required key from the remote, but it is old.");
			// able to verify self to remote, but change key
			connection.send(new KeyChange(localPublicKey, publicKey, decrypted, naunceRequest));
			localPublicKey = publicKey;
			return;
		}

		Services.logger.println("The remote has the correct key.");
		connection.send(new ConnectionOpenAwk(decrypted, naunceRequest));
	}

	public boolean newKey(Communication connection, PublicKey newKey)
	{
		if (authenticated != null 
				&& authenticated 
				&& remotePublicKey != null 
				&& Arrays.equals(newKey.getEncoded(), remotePublicKey.getEncoded()))
		{
			return true;
		}
		return acceptKey(connection, newKey, connection.getMachine());
	}

	public boolean acceptKey(Communication connection, PublicKey remote, Machine machine)
	{
		return acceptAnyKeys || Services.settings.acceptNewKeys.get() || AcceptKey.showAcceptDialog(
				connection.getUrl(),
				machine.getName(),
				machine.getIdentifier(),
				Misc.format(remote.getEncoded()));
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
