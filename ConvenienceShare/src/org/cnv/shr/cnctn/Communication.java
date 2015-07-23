
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



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
import java.util.HashMap;
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
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.PausableInputStream2;
import org.cnv.shr.util.PausableOutputStream;

import de.flexiprovider.core.rijndael.RijndaelKey;


// TODO: this should really only need authentication to UPDATE machine info...
public class Communication implements Closeable
{
	private static final int CLOSE_TIMEOUT = 1 * 60 * 1000;
	
	public static final boolean USE_ENCRYPTION = false;

	// The streams
	private Socket socket;
	
	// output wrapper layers
	private CountingInputStream countingInput;
	private CountingOutputStream countingOutput;
	
	private PausableInputStream2 pausableInput;
	private PausableOutputStream pausableOutput;
	
	private InputStream encryptedInput;
	private OutputStream encryptedOutput;
	
	// json stuff
	private JsonParser parser;
	private JsonGenerator generator;
	
	// true if this connection was initiated by the remote.
	private boolean receivedConnection;
	
	private String remoteIdentifier;
	private boolean needsMore;
	
	private Authenticator authentication;
	
	private ConnectionStatistics stats;
	
	private HashMap<String, Object> params = new HashMap<>();
	
	private static final boolean PRETTY_PRINT_ALL_COMMUNICATION = true;
	
	/** Initiator **/
	public Communication(Authenticator authentication, String ip, int port) throws UnknownHostException, IOException
	{
		this (createSocket(ip, port), authentication);
		receivedConnection = false;
	}
	private static Socket createSocket(String ip, int port) throws IOException { 
		Socket socket = new Socket();
		
		class TimeOutObject extends TimerTask
		{
			boolean done;

			@Override
			public void run()
			{
				if (done)
				{
					return;
				}
				try
				{
					socket.close();
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close on timeout.", e);
				}
			}
		}
		TimeOutObject obj = new TimeOutObject();
		Misc.timer.schedule(obj, 2000);
		socket.connect(new InetSocketAddress(ip, port), 2000);
		obj.done = true;
//		socket.setSoTimeout(2 * 60 * 1000);
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
		countingInput  = new CountingInputStream(socket.getInputStream());
		countingOutput = new CountingOutputStream(socket.getOutputStream());

		stats = new ConnectionStatistics(countingInput, countingOutput);
		
		pausableInput = new PausableInputStream2(countingInput);
		pausableOutput = new PausableOutputStream(countingOutput);
		
		needsMore = true;
		this.authentication = authentication;

		generator = TrackObjectUtils.createGenerator(pausableOutput, PRETTY_PRINT_ALL_COMMUNICATION);
		generator.writeStartObject();
		generator.flush();
	}
	
	void initParser()
	{
		parser = TrackObjectUtils.createParser(pausableInput);
		if (!parser.next().equals(JsonParser.Event.START_OBJECT))
		{
			throw new RuntimeException("Expected start object!");
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
		
		LogWrapper.getLogger().info("Sending message \"" + m.toString() + "\" to " + socket.getInetAddress() + ":" + socket.getPort());
		if (LogWrapper.getLogger().isLoggable(Level.FINE))
		{
			LogWrapper.getLogger().fine("The sent message was: " + m.getJsonKey() + ":" + m.toDebugString());
		}
		stats.setLastSent(m.getJsonKey());
		
		synchronized (getOutput())
		{
			// Should re-encrypt every so often...
			m.generate(generator, m.getJsonKey());
			generator.flush();
			pausableOutput.flush();
		}
	}

	public void flush() throws IOException
	{
		generator.flush();
		pausableOutput.flush();
	}

	public ConnectionStatistics getStatistics()
	{
		return stats;
	}

	public boolean isClosed()
	{
		return socket.isClosed();
	}

	public JsonGenerator getGenerator()
	{
		return generator;
	}
	public PausableOutputStream getOutput()
	{
		return pausableOutput;
	}
	public PausableInputStream2 getIn()
	{
		return pausableInput;
	}

	public void setRemoteIdentifier(String remoteIdentifier)
	{
//		if (this.remoteIdentifier != null && !remoteIdentifier.equals(this.remoteIdentifier))
//		{
//			Machine machine = DbMachines.getMachine(this.remoteIdentifier);
//			UserActions.assertUserAcceptsNewIdentifier(remoteIdentifier, machine, getUrl());
//		}
		if (Services.blackList.contains(remoteIdentifier))
		{
			LogWrapper.getLogger().info(remoteIdentifier + " is a blacklisted machine.");
			finish();
			return;
		}
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
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close socket.", e);
		}
	}

	// make this a method...
	public synchronized void setAuthenticated(boolean isAuthenticated) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IOException
	{
		if (isAuthenticated)
		{
			LogWrapper.getLogger().info("Remote is authenticated.");

			authentication.setAuthenticated(isAuthenticated);
			authentication.updateMachineInfo(remoteIdentifier, getIp());
			
			encrypt();
		}
		else
		{
			LogWrapper.getLogger().info("Remote failed authentication.");
			authentication.notifyAuthentication(null);
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
		catch (Exception e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to send done message.", e1);
		}
		finally
		{
			try
			{
				if (!socket.isOutputShutdown())
				{
					socket.shutdownOutput();
				}
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to close output.", e);
			}
			finally
			{
				scheduleTimerCloseTask();
			}
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to close input.", e);
		}
		finally
		{
			scheduleTimerCloseTask();
		}
	}
	
	private void scheduleTimerCloseTask()
	{
		try
		{
			Misc.timer.schedule(new TimerTask() { public void run() { closeDaConnection(); } }, CLOSE_TIMEOUT);
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to schedule shutdown timer", ex);
		}
	}
	
	public void closeDaConnection()
	{
		if (socket.isClosed())
		{
			return;
		}
		try
		{
			LogWrapper.getLogger().info("Connection close timer fired");
			socket.close();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close after expired.", e);
		}
	}

	public Authenticator getAuthentication()
	{
		return authentication;
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
	
	public synchronized void encrypt() throws InvalidKeyException, IOException
	{
		RijndaelKey key = KeysService.createAesKey();
		send(new NewAesKey(key, authentication.getRemoteKey()));
		generator.writeEnd();
		generator.close();
		

		if (USE_ENCRYPTION)
		{
			encryptedOutput = FlushableEncryptionStreams.createEncryptedOutputStream(countingOutput, key);
			pausableOutput.setDelegate(encryptedOutput);
			generator = TrackObjectUtils.createGenerator(pausableOutput, PRETTY_PRINT_ALL_COMMUNICATION);
		}
		else
		{
			encryptedOutput = new OutputStream() {
				@Override
				public void flush() throws IOException
				{
					countingOutput.flush();
				}
				@Override
				public void write(int b) throws IOException
				{
					countingOutput.write(b);
				}
				@Override
				public void write(byte[] b, int off, int len) throws IOException
				{
					countingOutput.write(b, off, len);
				}};
			pausableOutput.setDelegate(encryptedOutput);
			generator = TrackObjectUtils.createGenerator(pausableOutput, PRETTY_PRINT_ALL_COMMUNICATION);
		}
		
		generator.writeStartObject();
		generator.flush();
	}

	public synchronized void decrypt(RijndaelKey key) throws InvalidKeyException, IOException
	{
		if (!parser.next().equals(JsonParser.Event.END_OBJECT))
		{
			throw new RuntimeException("Expected end object!");
		}
		parser.close();
		
		if (USE_ENCRYPTION)
		{
			encryptedInput = FlushableEncryptionStreams.createEncryptedInputStream(countingInput, key);
			pausableInput.setDelegate(encryptedInput);
			pausableInput.startAgain();
			parser = TrackObjectUtils.createParser(pausableInput);
		}
		else
		{
			encryptedInput = new InputStream() {
				@Override
				public int read() throws IOException
				{
					return countingInput.read();
				}
				@Override
				public int available() throws IOException
				{
					return countingInput.available();
				}	
				public int read(byte[] arr, int off, int len) throws IOException
				{
					return countingInput.read(arr, off, len);
				}
			};
			pausableInput.setDelegate(encryptedInput);
			pausableInput.startAgain();
			parser = TrackObjectUtils.createParser(pausableInput);
		}
		
		if (!parser.next().equals(JsonParser.Event.START_OBJECT))
		{
			throw new RuntimeException("Expected start object!");
		}
		authentication.notifyAuthentication(this);
	}
	
	// Below methods should already be synchronized on out
	public void beginWriteRaw() throws IOException
	{
		generator.writeEnd();
		generator.close();
		pausableOutput.setRawMode(true);
	}
	public void endWriteRaw() throws IOException
	{
		pausableOutput.setRawMode(false);
		generator = TrackObjectUtils.createGenerator(pausableOutput, PRETTY_PRINT_ALL_COMMUNICATION);
		generator.writeStartObject();
		generator.flush();
	}
	public void beginReadRaw() throws IOException
	{
		if (!parser.next().equals(JsonParser.Event.END_OBJECT))
		{
			throw new RuntimeException("Expected end object!");
		}
		parser.close();
		pausableInput.startAgain();
		pausableInput.setRawMode(true);
	}
	public void endReadRaw() throws IOException
	{
		pausableInput.setRawMode(false);
		parser = TrackObjectUtils.createParser(pausableInput);
		if (!parser.next().equals(JsonParser.Event.START_OBJECT))
		{
			throw new RuntimeException("Expected start object!");
		}
	}
	public InetSocketAddress getRemoteSocketAddress()
	{
		return (InetSocketAddress) socket.getRemoteSocketAddress();
	}
	void setLastReceived(String jsonKey)
	{
		stats.setLastReceived(jsonKey);
	}
	public void setReason(String reason)
	{
		stats.setReason(reason);
	}
	
	// downloads should use this as well...
	public Object getParam(String str)
	{
		return params.get(str);
	}
	public void putParam(String str, Object obj)
	{
		params.put(str, obj);
	}
}
