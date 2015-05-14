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
import org.cnv.shr.msg.DoneResponse;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.OutputStreamFlusher;
import org.junit.internal.ExactComparisonCriteria;

import de.flexiprovider.core.rijndael.RijndaelKey;


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
	private OutputStreamFlusher flusher;
	
	// true if this connection was initiated by the remote.
	private boolean receivedConnection;
	
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
	
	/** Initiator **/
	public Communication(String ip, int port, boolean acceptKeys) throws UnknownHostException, IOException
	{
		acceptAnyKeys = acceptKeys;
		lastActivity = connectionOpened = System.currentTimeMillis();
		socket = new Socket(ip, port);
		output = socket.getOutputStream();
		input =  socket.getInputStream();
		receivedConnection = false;
	}
	
	/** Receiver **/
	public Communication(Socket socket) throws IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		this.socket = socket;
		input =  socket.getInputStream();
		output = socket.getOutputStream();
		receivedConnection = true;
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
			while (!done)
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
		
		send(new DoneResponse());
		close();
	}

	public synchronized void send(Message m)
	{
		new Exception().printStackTrace(System.out);
		if (m.requiresAthentication() && (authenticated == null || !authenticated))
		{
			throw new RuntimeException("Trying to send message on connection not yet authenticated. msg=" + m);
		}
		
		Services.logger.logStream.println("Sending message " + m + " to " + socket.getInetAddress() + ":" + socket.getPort());
		
		try
		{
			synchronized (output)
			{
				m.write(output);
				output.flush();
			}
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to send message: " + m.getClass().getName());
			e.printStackTrace(Services.logger.logStream);
		}
	}
	
	public void flush()
	{
		if (flusher != null)
		{
			flusher.flushPending();
		}
	}

	public boolean isClosed()
	{
		return socket.isClosed();
	}
	
	public synchronized void notifyDone()
	{
		send(new DoneMessage());
	}

	public void remoteIsDone()
	{
		done = true;
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
		Machine prevMachine = getMachine();
		if (prevMachine != null || Services.blackList.contains(claimedMachine))
		{
			Services.logger.logStream.println("There is already a machine with this id or it is on the blacklist");
			return;
		}
		// If we have no record of this machine, then it can be authenticated. (We will not share with it anyway.)
		// Otherwise, it must prove that it who it said it was in case we are sharing with it.
		

		// Somebody could spam a whole bunch of these to make the database too big.
		// Should fix that sometime...
		Services.logger.logStream.println("Found new machine!");
		claimedMachine = DbMachines.updateMachineInfo(claimedMachine, publicKeys, getIp());
	}

	public void notifyAuthentication(boolean authenticated, RijndaelKey aesKey)
	{
		lock.lock();
		try
		{
			condition.signalAll();
			if (authenticated)
			{
				Services.logger.logStream.println("Remote is authenticated.");
				Cipher cipherOut = Cipher.getInstance("AES128_CBC", "FlexiCore"); cipherOut.init(Cipher.ENCRYPT_MODE, aesKey);
				Cipher cipherIn  = Cipher.getInstance("AES128_CBC", "FlexiCore");  cipherIn.init(Cipher.DECRYPT_MODE, aesKey);

				if (receivedConnection)
				{
					input  = new CipherInputStream(input, cipherIn);
					output = new CipherOutputStream(flusher = new OutputStreamFlusher(this, output), cipherOut);
				}
				else
				{
					output = new CipherOutputStream(flusher = new OutputStreamFlusher(this, output), cipherOut);
					input  = new CipherInputStream(input, cipherIn);
				}
				
//				if (receivedConnection)
//				{
//					System.out.println("here...3.0");
//					input  = new GZIPInputStream(new CipherInputStream(input, cipherIn));
//					System.out.println("here...3.1");
//					for (int i = 0; i < flushLength; i++) input.read();
//					output = new GZIPOutputStream(new CipherOutputStream(output, cipherOut));
////					output = new GZIPOutputStream(new CipherOutputStream(flusher = new OutputStreamFlusher(this, output), cipherOut));
//					System.out.println("here...3.2");
//				}
//				else
//				{
////					output = new GZIPOutputStream(new CipherOutputStream(flusher = new OutputStreamFlusher(this, output), cipherOut));
//					output = new GZIPOutputStream(new CipherOutputStream(output, cipherOut));
//					for (int i = 0; i < flushLength; i++) output.write(0);
//					System.out.println("here...3.5");
////					flusher.flushPending();
//					input  = new GZIPInputStream(new CipherInputStream(input, cipherIn));
//					System.out.println("here...3.6");
//				}
				DbMachines.updateMachineInfo(claimedMachine, new PublicKey[] {remotePublicKey}, getIp());
			}
			else
			{
				Services.logger.logStream.println("Remote failed authentication.");
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
			localPublicKey = publicKey;
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

	public void close()
	{
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
}
