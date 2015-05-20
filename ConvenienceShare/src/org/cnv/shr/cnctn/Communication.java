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
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
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
	private InputStream input;
	private OutputStream output;
	private OutputStreamFlusher flusher;
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
		output = socket.getOutputStream();
		input =  socket.getInputStream();
		
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
		input =  socket.getInputStream();
		output = socket.getOutputStream();
		
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
			m.write(writer);
			output.flush();
		}
	}

	public void flush() throws IOException
	{
		if (flusher != null)
		{
			flusher.flushPending();
		}
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
	public void setAuthenticated(RijndaelKey aesKey) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException
	{
		authentication.notifyAuthentication(remoteIdentifier, getIp(), aesKey != null);
		
		if (aesKey == null)
		{
			return;
		}

		if (receivedConnection)
		{
			input  = FlushableEncryptionStreams.createEncryptedInputStream(input, aesKey);
			reader = new ByteReader(input, reader.getStatistics());
			output = FlushableEncryptionStreams.createEncryptedOutputStream(output, aesKey);
		}
		else
		{
			output = FlushableEncryptionStreams.createEncryptedOutputStream(output, aesKey);
			input  = FlushableEncryptionStreams.createEncryptedInputStream(input, aesKey);
			reader = new ByteReader(input, reader.getStatistics());
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
}
