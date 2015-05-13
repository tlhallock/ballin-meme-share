package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.util.Misc;


// TODO: this should really only need authentication to UPDATE machine info...
public class Communication implements Runnable
{
	// Statistics
	private long lastKbpsRefresh;
	private long numBytesSent;
	private long numBytesReceived;
	private long connectionOpened;
	private long lastActivity;
	
	// The streams
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	
	// The machine that the remote says it is
	private Machine claimedMachine;
	private PublicKey remotePublicKey;
	private PublicKey localPublicKey;
	
	// Authentication setting
	private boolean acceptAnyKeys;
	// Unecrypted naunces that the remote should be able to send back
	private HashSet<String> pendingNaunces = new HashSet<>();
	private Boolean authenticated;
	
	// a signal that we should stop
	private boolean done = false;
	
	// A condition to wait until this communication has been authenticated
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	// Keep the connection open while this is positive
	private int numUsers;
	
	/** Initiator **/
	public Communication(String ip, int port, boolean acceptKeys) throws UnknownHostException, IOException
	{
		acceptAnyKeys = acceptKeys;
		lastActivity = connectionOpened = System.currentTimeMillis();
		socket = new Socket(ip, port);
		output = socket.getOutputStream();
		input =  socket.getInputStream();
	}
	
	/** Receiver **/
	public Communication(Socket socket) throws IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		this.socket = socket;
		input =  socket.getInputStream();
		output = socket.getOutputStream();
	}

	public String getIp()
	{
		return socket.getInetAddress().getHostAddress();
	}
	public String getUrl()
	{
		return getIp() + ":" + socket.getPort();
	}
	
	public void run()
	{
		try
		{
			while (!done || numUsers > 0)
			{
				Message request = Services.msgReader.readMsg(this);
				if (request == null || !authenticate(request))
				{
					return;
				}
				
				lastActivity = System.currentTimeMillis();

				try
				{
					request.perform(this);
				}
				catch (Exception e)
				{
					Services.logger.logStream.println("Error performing message task:");
					e.printStackTrace(Services.logger.logStream);
				}
			}
		}
		catch (Exception ex)
		{
			Services.logger.logStream.println("Error with connection:");
			ex.printStackTrace(Services.logger.logStream);
		}
		notifyDone();
		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to close socket.");
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public synchronized void send(Message m)
	{
		if (m.requiresAthentication() && (authenticated == null || !authenticated))
		{
			throw new RuntimeException("Trying to send message on connection not yet authenticated");
		}
		
		Services.logger.logStream.println("Sending message of type " + m.getClass().getName()
				+ " to " + socket.getInetAddress() + ":" + socket.getPort());
		
		Services.logger.logStream.println("The message is " + m);
		
		try
		{
			synchronized (output)
			{
				m.write(output);
			}
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to send message: " + m.getClass().getName());
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public boolean isClosed()
	{
		return socket.isClosed();
	}
	
	public synchronized void addUser()
	{
		numUsers++;
	}
	
	public synchronized void notifyDone()
	{
		numUsers--;
		if (numUsers > 0)
		{
			return;
		}
		
		send(new DoneMessage());
	}

	public void remoteIsDone()
	{
		if (numUsers <= 0)
		{
			done = true;
		}
	}
	
	public boolean authenticate(Message m)
	{
		// Double check identifier and keys...
		return !m.requiresAthentication() || (authenticated != null && authenticated);
	}

	public OutputStream getOut()
	{
		return output;
	}
	public InputStream getIn()
	{
		return input;
	}

	public Machine getMachine()
	{
		return DbMachines.getMachine(claimedMachine.getIdentifier());
	}
	
	public long getKbs()
	{
		return 0;
	}

	public void updateKey(PublicKey publicKey)
	{
		this.remotePublicKey = publicKey;
	}

	public void setKeys(PublicKey remotePublicKey, PublicKey localPublicKey)
	{
		this.remotePublicKey = remotePublicKey;
		this.localPublicKey = localPublicKey;
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
	
	public boolean waitForAuthentication() throws IOException
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
				try
				{
					condition.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			returnValue = authenticated;
		}
		finally
		{
			lock.unlock();
		}
		return returnValue;
	}

	public void offerMachine(Machine claimedMachine, PublicKey[] publicKeys)
	{
		this.claimedMachine = claimedMachine;
		if (getMachine() == null && !Services.blackList.contains(claimedMachine))
		{
			// If we have no record of this machine, then it can be authenticated. (We will not share with it anyway.)
			// Otherwise, it must prove that it who it said it was in case we are sharing with it.
			

			// Somebody could spam a whole bunch of these to make the database too big.
			// Should fix that sometime...
			DbMachines.updateMachineInfo(claimedMachine, publicKeys, getIp());
		}
	}

	public void notifyAuthentication(boolean authenticated)
	{
		lock.lock();
		try
		{
			condition.signalAll();
			if (authenticated)
			{
				Cipher cipherOut = Cipher.getInstance("RSA", "FlexiCore"); cipherOut.init(Cipher.ENCRYPT_MODE, remotePublicKey);
				Cipher cipherIn  = Cipher.getInstance("RSA", "FlexiCore");  cipherIn.init(Cipher.DECRYPT_MODE, Services.keyManager.getPrivateKey(localPublicKey));
				
				
				// I thought this was stopping the stream from being flushed, but i guess it might be something else...
//				input  = new CipherInputStream(new GZIPInputStream(input), cipherIn);
//				output = new GZIPOutputStream(new CipherOutputStream(output, cipherOut));
				
				input  = new CipherInputStream(input, cipherIn);
				output = new CipherOutputStream(output, cipherOut);
				
				DbMachines.updateMachineInfo(claimedMachine, new PublicKey[]{remotePublicKey}, getIp());
			}
			
			this.authenticated = authenticated;
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			e.printStackTrace();
			Main.quit();
		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//			try
//			{
//				socket.close();
//			}
//			catch (IOException e1)
//			{
//				e1.printStackTrace();
//			}
//		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean acceptKey(PublicKey remote)
	{
		if (acceptAnyKeys || Services.settings.acceptNewKeys.get())
		{
			return true;
		}
		return Services.application != null && Services.application.acceptKey(getMachine(), remote);
	}
	
	public void authenticateToTarget(byte[] requestedNaunce) throws IOException
	{
		PublicKey publicKey = Services.keyManager.getPublicKey();
		if (!Services.keyManager.containsKey(localPublicKey))
		{
			// not able to verify self to remote, add key
			final byte[] sentNaunce = Services.keyManager.createTestNaunce(this, remotePublicKey);
			send(new NewKey(publicKey, sentNaunce));
			return;
		}
		
		byte[] decrypted = Services.keyManager.decryptNaunce(localPublicKey, requestedNaunce);
		byte[] naunceRequest = Services.keyManager.createTestNaunce(this, remotePublicKey);
		if (!Arrays.equals(publicKey.getEncoded(), localPublicKey.getEncoded()))
		{
			// able to verify self to remote, but change key
			send(new KeyChange(localPublicKey, publicKey, decrypted, naunceRequest));
			localPublicKey = publicKey;
			return;
		}

		send(new ConnectionOpenAwk(decrypted, naunceRequest));
	}
}
