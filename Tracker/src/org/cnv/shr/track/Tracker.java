
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



package org.cnv.shr.track;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.dmn.trk.NumFilesMessage;
import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.CompressionStreams;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.PausableInputStream2;
import org.cnv.shr.util.PausableOutputStream;

public class Tracker implements Runnable
{
	private TrackerStore store;
	private ServerSocket serverSocket;
	
	public Tracker(ServerSocket socket) throws SQLException
	{
		this.serverSocket = socket;
		store = new TrackerStore();
	}

	@Override
	public void run()
	{
		while (true)
		{
			if (serverSocket.isClosed())
			{
				break;
			}
			LogWrapper.getLogger().info("Waiting on " + serverSocket.getLocalPort());

			try (Socket socket = serverSocket.accept();)
			{
				PausableInputStream2 input  = new PausableInputStream2(/*LogStreams.newLogInputStream( */socket.getInputStream() /*, "socket")*/);
				PausableOutputStream output = new PausableOutputStream(/*LogStreams.newLogOutputStream(*/socket.getOutputStream()/*, "socket")*/);
				JsonParser parser       = TrackObjectUtils.createParser(input);
				JsonGenerator generator = TrackObjectUtils.createGenerator(output);
				
				EnsureClosed ensureClosed = new EnsureClosed();
				Misc.timer.schedule(ensureClosed, 10 * 60 * 1000);
				
				generator.writeStartArray();
				generator.flush();
				if (!parser.next().equals(JsonParser.Event.START_ARRAY))
				{
					fail("Messages should be arrays", parser, generator);
				}
				String hostName = socket.getInetAddress().getHostAddress();
				LogWrapper.getLogger().info("Connected to " + hostName);
				MachineEntry entry = authenticateClient(parser, generator, hostName);
				
				if (!parser.next().equals(JsonParser.Event.END_ARRAY))
				{
					throw new IOException("Expected end of old stream.");
				}
				parser.close();
				input.startAgain();
				parser = TrackObjectUtils.createParser(CompressionStreams.newCompressedInputStream(input));
				if (!parser.next().equals(JsonParser.Event.START_ARRAY))
				{
					throw new IOException("Client content did not start with an array.");
				}
				
				// Handshake done
				generator.writeEnd();
				generator.close();
				generator = TrackObjectUtils.createGenerator(CompressionStreams.newCompressedOutputStream(output));
				generator.writeStartArray();
				generator.flush();
				
				handleAction(generator, parser, entry);
				generator.writeEnd();
				generator.close();
				waitForClient(parser);
				ensureClosed.closed = true;
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Error with stream:", e);
			}
			catch (TrackerException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Bad message:", e);
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Error performing request:", e);
			}
		}
		
		store.close();
	}

	private void waitForClient(JsonParser input)
	{
		EnsureClosed ensureClosed = new EnsureClosed();
		Misc.timer.schedule(ensureClosed, 10 * 1000);
		try
		{
			input.next().equals(JsonParser.Event.END_ARRAY);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().info("Client already closed.\n" + e.getMessage());
		}
		finally
		{
			ensureClosed.closed = true;
		}
		
//		TrackObjectUtils.read(input, new Done());
	}

	private MachineEntry authenticateClient(JsonParser input, JsonGenerator generator, String realAddress) throws TrackerException, IOException
	{
		if (!getWantsToAuthenticate(input))
		{
			return null;
		}
		
		MachineEntry claimedClient = new MachineEntry();
		if (!TrackObjectUtils.read(input, claimedClient))
		{
			fail("Request with no Machine Entry", input, generator);
		}
		claimedClient.setIp(null);
		
		MachineEntry entry = store.getMachine(claimedClient.getIdentifer());
		if (entry == null || entry.getKeyStr() == null || entry.getKeyStr().length() == 0)
		{
			generator.writeStartObject();
			generator.write("need-authentication", false);
			generator.writeEnd();
			generator.flush();
			
			claimedClient.setIp(realAddress);
			store.machineFound(claimedClient, System.currentTimeMillis());
			return claimedClient;
		}
		
		generator.writeStartObject();
		generator.write("need-authentication", true);
		generator.writeEnd();
		
		// authenticate
		generator.writeStartObject();
		if (claimedClient.getKeyStr().equals(entry.getKeyStr()))
		{
			LogWrapper.getLogger().info("Keys matched.");
			generator.write("keysMatch", true);
		}
		else
		{
			LogWrapper.getLogger().info("Keys did not match.");
			generator.write("keysMatch", false);
			generator.write("prevKey", entry.getKeyStr());
		}

		byte[] createNaunce = Misc.createNaunce(117);
		byte[] encrypt = Track.keys.encrypt(entry.getKey(), createNaunce);
		generator.write("naunce", Misc.format(encrypt));
		generator.writeEnd();
		generator.flush();

		byte[] decrypted = getDecryptedNaunce(input, generator);
		if (!Arrays.equals(createNaunce, decrypted))
		{
			LogWrapper.getLogger().info("Client not authenticated.");
			return null;
		}

		LogWrapper.getLogger().info("Client authenticated.");
		claimedClient.setIp(realAddress);
		store.machineFound(claimedClient, System.currentTimeMillis());
		return claimedClient;
	}
	
	
	private static boolean getWantsToAuthenticate(JsonParser input) throws TrackerException
	{
		if (!input.next().equals(JsonParser.Event.START_OBJECT))
		{
			throw new TrackerException("Tracker connection needs to begin with an indicator for whether the client wishes to authenticate. No start.");
		}
		if (!input.next().equals(JsonParser.Event.KEY_NAME))
		{
			throw new TrackerException("Tracker connection needs to begin with an indicator for whether the client wishes to authenticate. No key name.");
		}
		if (!input.getString().equals("authenticate"))
		{
			throw new TrackerException("Tracker connection needs to begin with an indicator for whether the client wishes to authenticate. First key name not \"authenticate\".");
		}
		boolean returnValue;
		switch (input.next())
		{
		case VALUE_TRUE:
			returnValue = true;
			break;
		case VALUE_FALSE:
			returnValue = false;
			break;
			default:
				throw new TrackerException("Tracker connection needs to begin with an indicator for whether the client wishes to authenticate. First value not boolean.");
		}
		while (!input.next().equals(JsonParser.Event.END_OBJECT))
			;
		return returnValue;
	}

	private byte[] getDecryptedNaunce(JsonParser input, JsonGenerator generator) throws TrackerException
	{
		if (!input.next().equals(JsonParser.Event.START_OBJECT))
		{
			fail("Expected a decrypted naunce.", input, generator);
		}
		byte[] decrypted = null;
		String key = null;
		while (input.hasNext())
		{
			JsonParser.Event e = input.next();
			switch (e)
			{
			case KEY_NAME:
				key = input.getString();
				break;
			case VALUE_STRING:
				if (key == null) break;
				switch (key)
				{
				case "decrypted":    decrypted = Misc.format(input.getString()); break;
				}
				break;
			case END_OBJECT:
				return decrypted;
			default:
				break;
			}
		}
		return new byte[0];
	}

	private void handleAction(JsonGenerator output, JsonParser input, MachineEntry entry) throws TrackerException
	{
		TrackerRequest request = new TrackerRequest();
		if (!TrackObjectUtils.read(input, request))
		{
			fail("No request", input, output);
		}
		
		TrackerAction action = request.getAction();
		if (action == null)
		{
			fail("Request with no action.", input, output);
		}

		LogWrapper.getLogger().info("Tracker action: " + action.name());

		int offset = 0;
		FileEntry file;
		MachineEntry other;
		CommentEntry comment;
		switch (action)
		{
		case GET_MACHINE:
			other = store.getMachine(request.getParam("other"));
			output.writeStartArray();
			if (other != null)
			{
				other.generate(output);
			}
			output.writeEnd();
			break;
		case CLAIM_FILE:
			if (!Track.storesMetaData)
			{
				fail("This tracker does not store metadata.", input, output);
			}
			file = new FileEntry();
			TrackObjectUtils.openArray(input);
			if (entry == null)
			{
				fail("Need to authenticate before you can add a file.", input, output);
			}
			while (TrackObjectUtils.next(input, file))
			{
				LogWrapper.getLogger().info("Adding " + file.toString());
				store.machineClaims(entry, file);
			}
			break;
		case LIST_ALL_MACHINES:
			String param = request.getParam("offset");
			if (param != null)
			{
				try
				{
					offset = Integer.parseInt(param);
				}
				catch (NumberFormatException ex)
				{
					LogWrapper.getLogger().info("Bad offset: " + param);
				}
			}
			store.listMachines(new Lister<MachineEntry> (output), offset);
			break;
		case LIST_FILES:
			if (!Track.storesMetaData)
			{
				fail("This tracker does not store metadata.", input, output);
				return;
			}
			other = store.getMachine(request.getParam("other"));
			store.listFiles(other, new Lister<FileEntry>(output));
			break;
		case LIST_RATINGS:
			other = new MachineEntry();
			if (!TrackObjectUtils.read(input, other))
			{
				fail("List ratings without a machine", input, output);
				return;
			}
			param = request.getParam("offset");
			if (param != null)
			{
				try
				{
					offset = Integer.parseInt(param);
				}
				catch (NumberFormatException ex)
				{
					LogWrapper.getLogger().info("Bad offset: " + param);
				}
			}
			new NumFilesMessage(store.getNumFiles(other)).generate(output, null);
			store.listComments(other, new Lister<CommentEntry>(output), offset);
			break;
		case LIST_SEEDERS:
			if (!Track.storesMetaData)
			{
				fail("This tracker does not store metadata.", input, output);
			}
			file = new FileEntry();
			if (!TrackObjectUtils.read(input, file))
			{
				fail("List without a file", input, output);
			}
			store.listSeeders(file, new Lister<MachineEntry> (output));
			break;
		case LIST_TRACKERS:
			store.listTrackers(new Lister<TrackerEntry>(output));
			break;
		case LOSE_FILE:
			file = new FileEntry();
			TrackObjectUtils.openArray(input);
			if (entry == null)
			{
				fail("Need to authenticate before you can remove a file.", input, output);
			}
			while (TrackObjectUtils.next(input, file))
			{
				LogWrapper.getLogger().info("Removing " + file.toString());
				store.machineLost(entry, file);
			}
			break;
		case POST_COMMENT:
			if (entry == null)
			{
				fail("Need to authenticate before you can post a comment", input, output);
			}
			comment = new CommentEntry();
			if (!TrackObjectUtils.read(input, comment))
			{
				fail("Post without a comment.", input, output);
			}
			LogWrapper.getLogger().info(comment.toString());
			comment.setOrigin(entry.getIdentifer());
			store.postComment(comment);
			break;
		case POST_MACHINE:
			if (entry == null)
			{
				fail("Need to authenticate before add yourself to the tracker.", input, output);
			}
			// already done...
			break;
		default:
			fail("Unkown TrackAction: " + action, input, output);
		}
	}
	
	private void fail(String message, JsonParser input, JsonGenerator generator) throws TrackerException
	{
		LogWrapper.getLogger().info("Fail: " + message);
		
		generator.writeStartObject();
		generator.write("errorMsg", message);
		generator.writeEnd();
		generator.flush();

		waitForClient(input);
		throw new TrackerException(message);
	}

	class EnsureClosed extends TimerTask
	{
		Thread t;
		
		public EnsureClosed()
		{
			this.t = Thread.currentThread();
		}
		boolean closed = false;
		@Override
		public void run()
		{
			if (closed) return;
			t.interrupt();
		}
	}
}
