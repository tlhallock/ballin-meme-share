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
import java.util.TimerTask;
import java.util.logging.Level;

import javax.crypto.NoSuchPaddingException;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.dwn.NewAesKey;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.ConnectionStatistics;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.CountingOutputStream;
import org.cnv.shr.util.FlushableEncryptionStreams;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;

import de.flexiprovider.core.rijndael.RijndaelKey;


// TODO: this should really only need authentication to UPDATE machine info...
public class Communication implements Closeable
{
//	private JsonGenerator logFile; 
//	{ 
//    Map<String, Object> properties = new HashMap<>(1);
//    properties.put(JsonGenerator.PRETTY_PRINTING, true);
//		logFile = Json.createGeneratorFactory(properties).createGenerator(Files.newOutputStream(Paths.get("log." + System.currentTimeMillis() + "." + Math.random() + ".txt")));
//		logFile.writeStartArray();
//	}
	// The streams
	private Socket socket;
	
	private InputStream inputOrig;
	private OutputStream outputOrig;
	private JsonParser parser;
	private JsonGenerator generator;
	
	// true if this connection was initiated by the remote.
	private boolean receivedConnection;
	
	private String remoteIdentifier;
	private boolean needsMore;
	
	private Authenticator authentication;
	
	ConnectionStatistics stats;
	
	/** Initiator **/
	public Communication(Authenticator authentication, String ip, int port) throws UnknownHostException, IOException
	{
		this (createSocket(ip, port), authentication);
		receivedConnection = false;
	}
	private static Socket createSocket(String ip, int port) throws IOException { 
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(ip, port), 2000);
		return socket;
	}
	
	/** Receiver **/
	public Communication(Authenticator authentication, Socket socket) throws IOException
	{
		this (socket, authentication);
		receivedConnection = true;
	}
	
	private Communication(Socket socket, Authenticator authentication) throws IOException
	{
		this.socket = socket;
		inputOrig  = new CountingInputStream(socket.getInputStream());
		outputOrig = new CountingOutputStream(socket.getOutputStream());

		stats = new ConnectionStatistics((CountingInputStream) inputOrig, (CountingOutputStream) outputOrig);
		
		needsMore = true;
		this.authentication = authentication;

		generator = TrackObjectUtils.createGenerator(outputOrig);
		generator.writeStartArray();
	}
	
	void initParser()
	{
		parser = TrackObjectUtils.createParser(inputOrig);
		if (!parser.next().equals(JsonParser.Event.START_ARRAY))
		{
			System.out.println("This is bad...");
		}
	}
	
	public JsonParser getParser()
	{
		return parser;
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
		if (socket.isOutputShutdown())
		{
			LogWrapper.getLogger().info("Connection closed. cant send " + m);
			return;
		}
		
		authentication.assertCanSend(m);
		
		LogWrapper.getLogger().info("Sending message \"" + m + "\" to " + socket.getInetAddress() + ":" + socket.getPort());

		synchronized (generator)
		{
			// Should re-encrypt every so often...
			m.generate(generator);
			generator.flush();
			outputOrig.flush();
		}
	}

	public void flush() throws IOException
	{
		generator.flush();
		outputOrig.flush();
	}

	public ConnectionStatistics getStatistics()
	{
		stats.refresh();
		return stats;
	}

	public boolean isClosed()
	{
		return socket.isClosed();
	}

	public OutputStream getOut()
	{
		return outputOrig;
	}
	public InputStream getIn()
	{
		return inputOrig;
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to close socket.", e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to close output.", e);
		}
		Services.timer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				if (socket.isClosed())
					return;
				try
				{
					socket.close();
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close after expired.", e);
				}
			}
		}, 1 * 60 * 1000);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to close input.", e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to close in finalize", e);
		}
	}
	
	/** Don't let these be finalized. **/
	private static JsonGenerator oldGen;
	private static JsonParser oldParser;
	public synchronized void encrypt() throws InvalidKeyException, IOException
	{
		RijndaelKey key = KeysService.createAesKey();
		send(new NewAesKey(key, authentication.getRemoteKey()));
		generator.writeEnd();
		generator.flush();
		oldGen = generator;
		generator = TrackObjectUtils.createGenerator(outputOrig = FlushableEncryptionStreams.createEncryptedOutputStream(outputOrig, key));
		generator.writeStartArray();
	}

	public synchronized void decrypt(RijndaelKey key) throws InvalidKeyException
	{
		if (!parser.next().equals(JsonParser.Event.END_ARRAY))
		{
			System.out.println("This is bad...");
		}
		oldParser = parser;
		parser = TrackObjectUtils.createParser(inputOrig = FlushableEncryptionStreams.createEncryptedInputStream(inputOrig, key));
		if (!parser.next().equals(JsonParser.Event.START_ARRAY))
		{
			System.out.println("This is bad...");
		}
	}
}
