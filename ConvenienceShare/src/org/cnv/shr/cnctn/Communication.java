package org.cnv.shr.cnctn;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.OutputStreamFlusher;

import de.flexiprovider.core.rijndael.RijndaelKey;


// TODO: this should really only need authentication to UPDATE machine info...
public class Communication implements Closeable
{
	// The streams
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private OutputStreamFlusher flusher;
	
	// true if this connection was initiated by the remote.
	private boolean receivedConnection;
	
	private String remoteIdentifier;
	private boolean needsMore;
	
	private AuthenticationWaiter authentication;
	
	/** Initiator **/
	public Communication(AuthenticationWaiter authentication, String ip, int port) throws UnknownHostException, IOException
	{
		socket = new Socket(ip, port);
		output = socket.getOutputStream();
		input =  socket.getInputStream();
		receivedConnection = false;
		needsMore = true;
		this.authentication = authentication;
	}
	
	/** Receiver **/
	public Communication(AuthenticationWaiter authentication, Socket socket) throws IOException
	{
		this.socket = socket;
		input =  socket.getInputStream();
		output = socket.getOutputStream();
		receivedConnection = true;
		needsMore = true;
		this.authentication = authentication;
	}

	public String getIp()
	{
		return socket.getInetAddress().getHostAddress();
	}
	public String getUrl()
	{
		return getIp() + ":" + socket.getPort();
	}
	
	public synchronized void send(Message m)
	{
		authentication.assertCanSend(m);
		
		Services.logger.logStream.println("Sending message \"" + m + "\" to " + socket.getInetAddress() + ":" + socket.getPort());
		
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

	public OutputStream getOut()
	{
		return output;
	}
	public InputStream getIn()
	{
		return input;
	}

	public void setRemoteIdentifier(String remoteIdentifier)
	{
		this.remoteIdentifier = remoteIdentifier;
	}
	
	public Machine getMachine()
	{
		return DbMachines.getMachine(remoteIdentifier);
	}
	
	public void close()
	{
		try
		{
			if (!socket.isClosed())
			{
				socket.close();
			}
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to close socket.");
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public void setAuthenticated(RijndaelKey aesKey) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException
	{
		authentication.notifyAuthentication(remoteIdentifier, getIp(), aesKey != null);
		
		if (aesKey == null)
		{
			return;
		}
		
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
	}

	boolean needsMore()
	{
		return needsMore;
	}

	public void finish()
	{
		send(new DoneMessage());
		try
		{
			socket.shutdownOutput();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void setDone()
	{
		try
		{
			socket.shutdownInput();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public AuthenticationWaiter getAuthentication()
	{
		return authentication;
	}

	public Socket getSocket()
	{
		return socket;
	}
	
	// todo: remove this. can be done by breaking up whoiam and changing runnable exception to throwable
	public void finalize()
	{
		try
		{
			if (!socket.isClosed()) socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
