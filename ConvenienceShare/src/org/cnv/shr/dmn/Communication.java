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
	private long connectionOpened;
	private long lastActivity;
	
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	
	private Machine machine;
	PublicKey remotePublicKey;
	PublicKey localPublicKey;
	
	private boolean acceptAnyKeys;
	private HashSet<String> pendingNaunces = new HashSet<>();
	private Boolean authenticated;
	
	private boolean done = false;
	
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
	}
	
	/** Receiver **/
	public Communication(Socket socket) throws IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		this.socket = socket;
		input =  socket.getInputStream();
		output = socket.getOutputStream();
	}
	
	public String getUrl()
	{
		return socket.getInetAddress() + ":" + socket.getPort();
	}
	
	public void run()
	{
		try
		{
			while (!done)
			{
				Message request = Services.msgReader.readMsg(socket.getInetAddress(), this);
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

	public void send(Message m)
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
				output.write(m.getBytes());
				output.flush();
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
	
	public void notifyDone()
	{
		send(new DoneMessage());
//		try
//		{
//			socket.shutdownOutput();
//		}
//		catch (IOException e)
//		{
//			Services.logger.logStream.println("Unable to close output stream.");
//			e.printStackTrace(Services.logger.logStream);
//		}
	}

	public void remoteIsDone()
	{
		done = true;
//		try
//		{
//			socket.shutdownInput();
//		}
//		catch (IOException e)
//		{
//			Services.logger.logStream.println("Unable to close input stream.");
//			e.printStackTrace(Services.logger.logStream);
//		}
	}
	
	public boolean authenticate(Message m)
	{
		// Double check identifier and keys...
		return m.requiresAthentication() || (authenticated != null && authenticated);
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
		return machine;
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
	
	public boolean waitForAuthentication()
	{
		lock.lock();
		boolean returnValue = false;
		try
		{
			while (authenticated == null)
			{
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

	public void notifyAuthentication(boolean authenticated)
	{
		lock.lock();
		try
		{
			if (authenticated)
			{
				Cipher cipherOut = Cipher.getInstance("RSA", "FlexiCore"); cipherOut.init(Cipher.ENCRYPT_MODE, remotePublicKey);
				Cipher cipherIn  = Cipher.getInstance("RSA", "FlexiCore");  cipherIn.init(Cipher.DECRYPT_MODE, Services.keyManager.getPrivateKey(localPublicKey));
				
				input  = new CipherInputStream(new GZIPInputStream(input), cipherIn);
				output = new GZIPOutputStream(new CipherOutputStream(output, cipherOut));
				
				// update machine info from message
			}
			
			this.authenticated = authenticated;
			condition.signalAll();
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			e.printStackTrace();
			Main.quit();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			condition.signalAll();
			try
			{
				socket.close();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
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
		return Services.application != null && Services.application.acceptKey(machine, remote);
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
