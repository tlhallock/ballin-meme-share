//
///*                                                                          *
// * Copyright (C) 2015    Trever Hallock                                     *
// *                                                                          *
// * This program is free software; you can redistribute it and/or modify     *
// * it under the terms of the GNU General Public License as published by     *
// * the Free Software Foundation; either version 2 of the License, or        *
// * (at your option) any later version.                                      *
// *                                                                          *
// * This program is distributed in the hope that it will be useful,          *
// * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
// * GNU General Public License for more details.                             *
// *                                                                          *
// * You should have received a copy of the GNU General Public License along  *
// * with this program; if not, write to the Free Software Foundation, Inc.,  *
// * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
// *                                                                          *
// * See LICENSE file at repo head at                                         *
// * https://github.com/tlhallock/ballin-meme-share                           *
// * or after                                                                 *
// * git clone git@github.com:tlhallock/ballin-meme-share.git                 */
//
//
//
//package org.cnv.shr.cnctn;
//
//import java.io.IOException;
//import java.security.PublicKey;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.logging.Level;
//
//import org.cnv.shr.db.h2.DbKeys;
//import org.cnv.shr.db.h2.DbMachines;
//import org.cnv.shr.dmn.Services;
//import org.cnv.shr.gui.AcceptKey;
//import org.cnv.shr.mdl.Machine;
//import org.cnv.shr.msg.Message;
//import org.cnv.shr.msg.key.ConnectionOpenAwk;
//import org.cnv.shr.msg.key.KeyChange;
//import org.cnv.shr.msg.key.NewKey;
//import org.cnv.shr.util.LogWrapper;
//import org.cnv.shr.util.Misc;
//import org.cnv.shr.util.MissingKeyException;
//
//public class Authenticator
//{
//	private Boolean authenticated;
//	private HashSet<String> pendingNaunces = new HashSet<>();
//
//	// The machine that the remote says it is
//	private PublicKey localPublicKey;
//	
//	private String claimedName;
//	private int claimedPort;
//	private int claimedNumPorts;
//
//	private ConnectionParams params;
//	
//	public Authenticator(ConnectionParams params)
//	{
//		this.params = params;
//		this.localPublicKey = Services.keyManager.getPublicKey();
//	}
//
//	public Authenticator()
//	{
//		this.params = new ConnectionParams() {
//			@Override
//			public void opened(Communication connection) throws Exception {}
//			@Override
//			public boolean closeWhenDone() { return false; }
//		};
//		this.localPublicKey = Services.keyManager.getPublicKey();
//	}
//
//	public void setMachineInfo(String name, int port, int nports)
//	{
//		this.claimedName = name;
//		this.claimedPort = port;
//		this.claimedNumPorts = nports;
//	}
//	
//	public void offerRemote(String id, String ip, PublicKey key)
//	{
//		// If we have no record of this machine, then it can be authenticated. (We will not share with it anyway.)
//		// Otherwise, it must prove that it who it said it was in case we are sharing with it.
//		// Somebody could spam a whole bunch of these to make the database too big.
//		// Should fix that sometime...
//		if (id.equals(Services.localMachine.getIdentifier()))
//		{
//			return;
//		}
//
//		if (params.remoteKey == null && key != null)
//		{
//			params.remoteKey = key;
//		}
//
//		if (DbMachines.getMachine(id) != null)
//		{
//			return;
//		}
//		
//		LogWrapper.getLogger().info("Found new machine!");
//		updateMachineInfo(id, ip, key);
//	}
//	
//	void updateMachineInfo(String ident, String ip, PublicKey key)
//	{
//		DbMachines.updateMachineInfo(ident, claimedName, key, ip, claimedPort, claimedNumPorts);
//	}
//
//	public void setAuthenticated(boolean isAuthenticated)
//	{
//		this.authenticated = isAuthenticated;
//	}
//	
//	void notifyAuthentication(Communication connection)
//	{
//		if (!authenticated)
//		{
//			Services.userThreads.execute(() -> { params.notifyFailed(); });
//			return;
//		}
//		Services.userThreads.execute(() -> {
//			try
//			{
//				params.notifyOpened(connection);
//				if (params.closeWhenDone())
//				{
//					connection.finish();
//				}
//			}
//			catch (Exception e)
//			{
//				LogWrapper.getLogger().log(Level.INFO, "Unable to execute callback: " + params.reason, e);
//				connection.finish();
//			}
//		});
//	}
//
//	public boolean authenticate(Message m)
//	{
//		// Double check identifier and keys...
//		return !m.requiresAthentication() || authenticated != null && authenticated;
//	}
//	
//	public void setLocalKey(PublicKey localKey)
//	{
//		this.localPublicKey = localKey;
//	}
//	public void setRemoteKey(PublicKey publicKey)
//	{
//		params.remoteKey = publicKey;
//	}
//	
//	public PublicKey getLocalKey()
//	{
//		return localPublicKey;
//	}
//	public PublicKey getRemoteKey()
//	{
//		return params.remoteKey;
//	}
//
//	public void addPendingNaunce(byte[] original)
//	{
//		pendingNaunces.add(Misc.format(original));
//	}
//
//	public boolean hasPendingNaunce(byte[] decrypted)
//	{
//		boolean returnValue = pendingNaunces.contains(Misc.format(decrypted));
//		pendingNaunces.clear();
//		return returnValue;
//	}
//	
//	public boolean canAuthenticateRemote(Communication connection, PublicKey remote, PublicKey local) throws IOException
//	{
//		this.localPublicKey = local;
//		params.remoteKey = remote;
//		
//		Machine machine = connection.getMachine();
//		
//		// authenticate remote...
//		if (DbKeys.machineHasKey(machine, remote))
//		{
//			LogWrapper.getLogger().info("We have a the key for the remote.");
//			return true;
//		}
//
//		if (acceptKey(connection, remote, machine))
//		{
//			LogWrapper.getLogger().info("We have accepted the key for the remote.");
//			DbKeys.addKey(machine, remote);
//			return true;
//		}
//
//		LogWrapper.getLogger().info("Unable to accept remote key.");
//		// add message
//		return false;
//	}
//	
//	public void authenticateToTarget(Communication connection, byte[] requestedNaunce) throws IOException, MissingKeyException
//	{
//		PublicKey publicKey = Services.keyManager.getPublicKey();
//		if (!Services.keyManager.containsKey(localPublicKey))
//		{
//			LogWrapper.getLogger().info("The remote's key for us will not do.");
//			// not able to verify self to remote, add key
//			localPublicKey = publicKey;
//			final byte[] sentNaunce = IdkWhereToPutThis.createTestNaunce(this, params.remoteKey);
//			connection.send(new NewKey(publicKey, sentNaunce));
//			return;
//		}
//		
//		byte[] decrypted = Services.keyManager.decrypt(localPublicKey, requestedNaunce);
//		byte[] naunceRequest = IdkWhereToPutThis.createTestNaunce(this, params.remoteKey);
//		if (!Arrays.equals(publicKey.getEncoded(), localPublicKey.getEncoded()))
//		{
//			LogWrapper.getLogger().info("We have the required key from the remote, but it is old.");
//			// able to verify self to remote, but change key
//			connection.send(new KeyChange(localPublicKey, publicKey, decrypted, naunceRequest));
//			localPublicKey = publicKey;
//			return;
//		}
//
//		LogWrapper.getLogger().info("The remote has the correct key.");
//		connection.send(new ConnectionOpenAwk(decrypted, naunceRequest));
//	}
//
//	public boolean newKey(Communication connection, PublicKey newKey)
//	{
//		if (authenticated != null 
//				&& authenticated 
//				&& params.remoteKey != null 
//				&& Arrays.equals(newKey.getEncoded(), params.remoteKey.getEncoded()))
//		{
//			return true;
//		}
//		return acceptKey(connection, newKey, connection.getMachine());
//	}
//
//	public boolean acceptKey(Communication connection, PublicKey remote, Machine machine)
//	{
//		if (remote == null) return false;
//		return params.acceptAllKeys || Services.settings.acceptNewKeys.get() || AcceptKey.showAcceptDialog(
//				connection.getUrl(),
//				machine.getName(),
//				machine.getIdentifier(),
//				Misc.format(remote.getEncoded()));
//	}
//
//	public void assertCanSend(Message m)
//	{
//		if (m.requiresAthentication() && (authenticated == null || !authenticated))
//		{
//			throw new RuntimeException("Trying to send message on connection not yet authenticated.\n"
//					+ "type = " + m.getClass().getName() + "\nmsg=\"" + m + "\"");
//		}
//	}
//
//	public void updateMachineInfo(String remoteIdentifier, String ip)
//	{
//		updateMachineInfo(remoteIdentifier, ip, params.remoteKey);
//	}
//}
