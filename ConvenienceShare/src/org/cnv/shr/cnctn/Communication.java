package org.cnv.shr.cnctn;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.KeysService;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.dwn.NewAesKey;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.FlushableEncryptionStreams;
import org.cnv.shr.util.OutputByteWriter;
import org.cnv.shr.util.OutputStreamFlusher;

import de.flexiprovider.core.rijndael.RijndaelKey;


// TODO: this should really only need authentication to UPDATE machine info...
public class Communication implements Closeable
{
	// The streams
	private Socket socket;
	
	private InputStream inputOrig;
	private OutputStream outputOrig;
	private InputStream input;
	private OutputStream output;
	
	private ByteReader reader;
	private OutputByteWriter writer;
	
	// true if this connection was initiated by the remote.
	private boolean receivedConnection;
	
	private String remoteIdentifier;
	private boolean needsMore;
	
	private Authenticator authentication;
	
	/** Initiator **/
	public Communication(Authenticator authentication, String ip, int port) throws UnknownHostException, IOException
	{
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip, port), 2000);
		outputOrig = output = socket.getOutputStream();
		inputOrig  = input =  socket.getInputStream();
		
		ConnectionStatistics stats = new ConnectionStatistics();
		reader = new ByteReader(input, stats);
		writer = new OutputByteWriter(output, stats);
		
		receivedConnection = false;
		needsMore = true;
		this.authentication = authentication;
	}
	
	/** Receiver **/
	public Communication(Authenticator authentication, Socket socket) throws IOException
	{
		this.socket = socket;
		inputOrig  = input =  socket.getInputStream();
		outputOrig = output = socket.getOutputStream();
		
		ConnectionStatistics stats = new ConnectionStatistics();
		reader = new ByteReader(input, stats);
		writer = new OutputByteWriter(output, stats);
		
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
	
	public synchronized void send(Message m) throws IOException
	{
		authentication.assertCanSend(m);
		
		Services.logger.println("Sending message \"" + m + "\" to " + socket.getInetAddress() + ":" + socket.getPort());

		synchronized (output)
		{
			// Should re-encrypt every so often...
			m.write(writer);
			output.flush();
		}
	}

	public void flush() throws IOException
	{
		output.flush();
	}

	public ConnectionStatistics getStatistics()
	{
		return this.getReader().getStatistics();
	}

	public boolean isClosed()
	{
		return socket.isClosed();
	}

	public ByteReader getReader()
	{
		return reader;
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
		if (remoteIdentifier == null) return null;
		return DbMachines.getMachine(remoteIdentifier);
	}
	
	@Override
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
			Services.logger.println("Unable to close socket.");
			Services.logger.print(e);
		}
	}

	// make this a method...
	public synchronized void setAuthenticated(boolean isAuthenticated) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IOException
	{
		authentication.notifyAuthentication(remoteIdentifier, getIp(), isAuthenticated);
		if (isAuthenticated)
		{
			encrypt();
		}
	}

	boolean needsMore()
	{
		return needsMore;
	}

	public void finish()
	{
		try
		{
			send(new DoneMessage());
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			socket.shutdownOutput();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}
	
	public void setDone()
	{
		try
		{
			needsMore = false;
			socket.shutdownInput();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}

	public Authenticator getAuthentication()
	{
		return authentication;
	}

	public Socket getSocket()
	{
		return socket;
	}
	
	// todo: remove this. can be done by breaking up whoiam and changing runnable exception to throwable
	@Override
	public void finalize()
	{
		try
		{
			if (!socket.isClosed()) socket.close();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}
	
	
	public synchronized void encrypt() throws InvalidKeyException, IOException
	{
		RijndaelKey key = KeysService.createAesKey();
		send(new NewAesKey(key, authentication.getRemoteKey()));
		output.flush();
		output = FlushableEncryptionStreams.createEncryptedOutputStream(outputOrig, key);
		writer = new OutputByteWriter(output);
	}
	
	public synchronized void decrypt(RijndaelKey key) throws InvalidKeyException
	{
		input = FlushableEncryptionStreams.createEncryptedInputStream(input, key);
		reader = new ByteReader(input, reader.getStatistics());
	}
}
