package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.InitiateAuthentication;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.util.Misc;

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
	
	private HashMap<String, byte[]> pendingNaunces = new HashMap<>();
	private boolean authenticated;
	
	
	
	private boolean done = false;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	/** Initiator **/
	public Communication(String ip, int port) throws UnknownHostException, IOException
	{
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
				Message request = Services.msgReader.readMsg(socket.getInetAddress(), input);
				if (request == null || !request.authenticate())
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
		Services.logger.logStream.println("Sending message of type " + m.getClass().getName()
				+ " to " + socket.getInetAddress() + ":" + socket.getPort());
		
		Services.logger.logStream.println("The message is " + m);
		
		try
		{
			synchronized (output)
			{
				output.write(m.getBytes());
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

	public void addPendingNaunce(byte[] naunce)
	{
		String pKey = Misc.format(naunce);
		pendingNaunces.put(pKey, naunce);
	}

	public byte[] getPendingNaunce(PublicKey key)
	{
		String keyVal = Misc.format(key.getEncoded());
		byte[] returnValue = pendingNaunces.get(keyVal);
		pendingNaunces.remove(keyVal);
		return returnValue;
	}

	public void isAuthenticated()
	{
		authenticated = true;
	}
	
	public void authenticateToTarget(byte[] requestedNaunce)
	{
		PublicKey publicKey = Services.keyManager.getPublicKey();
		if (!Services.keyManager.containsKey(localPublicKey))
		{
			// not able to verify self to remote, add key
			byte[] encoded = Services.keyManager.encode(publicKey, requestedNaunce);

			final byte[] original = Misc.createNaunce();
			final byte[] sentNaunce = Services.keyManager.createNaunce(publicKey, original);
			addPendingNaunce(original);
			send(new NewKey(publicKey, encoded, sentNaunce));
			return;
		}
		
		if (!Arrays.equals(publicKey.getEncoded(), localPublicKey.getEncoded()))
		{
			// able to verify self to remote, but change key
			send(new KeyChange(localPublicKey, publicKey, encoded, Services.keyManager.createTestNaunce(this, publicKey)));
			localPublicKey = publicKey;
			return;
		}

		byte[] encoded = Services.keyManager.encode(localPublicKey, requestedNaunce);
		byte[] responseAwk = Misc.getNaunce();
		addPendingNaunce(responseAwk);
		send(new ConnectionOpenAwk(encoded, responseAwk));
	}
}
