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

import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

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
			
			try (Socket socket       = serverSocket.accept();
					JsonParser input     = TrackObjectUtils.createParser(socket.getInputStream());
					JsonGenerator output = TrackObjectUtils.createGenerator(socket.getOutputStream());)
			{
				EnsureClosed ensureClosed = new EnsureClosed(output);
				Track.timer.schedule(ensureClosed, 10 * 60 * 1000);
				
				output.writeStartArray();
				if (!input.next().equals(JsonParser.Event.START_ARRAY))
				{
					fail("Messages should be arrays", input, output);
				}
				String hostName = socket.getInetAddress().getHostAddress();
				LogWrapper.getLogger().info("Connected to " + hostName);
				MachineEntry entry = authenticateClient(input, output, hostName);
				handleAction(output, input, entry);
				output.writeEnd();
				waitForClient(input, output);
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

	private void waitForClient(JsonParser input, JsonGenerator generator)
	{
		generator.flush();
		EnsureClosed ensureClosed = new EnsureClosed(generator);
		Track.timer.schedule(ensureClosed, 10 * 1000);
		input.next().equals(JsonParser.Event.END_ARRAY);
		ensureClosed.closed = true;
		
//		TrackObjectUtils.read(input, new Done());
	}

	private MachineEntry authenticateClient(JsonParser input, JsonGenerator generator, String realAddress) throws TrackerException
	{
		MachineEntry claimedClient = new MachineEntry();
		if (!TrackObjectUtils.read(input, claimedClient))
		{
			fail("Request with no Machine Entry", input, generator);
		}
		claimedClient.setIp(null);
		
		MachineEntry entry = store.getMachine(claimedClient.getIdentifer());
		if (entry == null)
		{
			generator.writeStartObject();
			generator.write("need-authentication", false);
			generator.writeEnd();
			
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
			// this should be a stream...
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
			store.listMachines(output);
			break;
		case LIST_MY_FILES:
			other = store.getMachine(request.getParam("other"));
			store.listFiles(other, output);
			break;
		case LIST_RATINGS:
			other = new MachineEntry();
			if (!TrackObjectUtils.read(input, other))
			{
				fail("List ratings without a machine", input, output);
			}
			store.listComments(other, output);
			break;
		case LIST_SEEDERS:
			file = new FileEntry();
			if (!TrackObjectUtils.read(input, file))
			{
				fail("List without a file", input, output);
			}
			int offset = 0;
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
			store.listMachines(file, output, offset);
			break;
		case LIST_TRACKERS:
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

		waitForClient(input, generator);
		throw new TrackerException(message);
	}

	class EnsureClosed extends TimerTask
	{
		JsonGenerator generator;
		Thread t;
		
		public EnsureClosed(JsonGenerator generator)
		{
			this.generator = generator;
			this.t = Thread.currentThread();
		}
		boolean closed = false;
		@Override
		public void run()
		{
			if (closed) return;
			generator.close();
			t.interrupt();
		}
	}
}
