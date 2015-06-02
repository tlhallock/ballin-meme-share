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

import org.cnv.shr.trck.Comment;
import org.cnv.shr.trck.Done;
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

	public void run()
	{
		while (true)
		{
			try (Socket socket       = serverSocket.accept();
					JsonGenerator output = TrackObjectUtils.generatorFactory.createGenerator(socket.getOutputStream());
					JsonParser input     = TrackObjectUtils.parserFactory.createParser(socket.getInputStream());)
			{
				MachineEntry entry = authenticateClient(input, output);
				handleAction(output, input, entry);
				waitForClient(input);
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
	}

	private void waitForClient(JsonParser input)
	{
		TrackObjectUtils.read(input, new Done());
	}

	private MachineEntry authenticateClient(JsonParser input, JsonGenerator generator) throws TrackerException
	{
		MachineEntry claimedClient = new MachineEntry();
		if (!TrackObjectUtils.read(input, claimedClient))
		{
			fail("Request with Machine Entry", input, generator);
		}
		
		MachineEntry entry = store.getMachine(claimedClient.getIdentifer());
		if (entry == null)
		{
			store.machineFound(claimedClient, System.currentTimeMillis());
			return claimedClient;
		}
		
		// authenticate
		generator.writeStartObject();
		if (claimedClient.getKeyStr().equals(entry.getKeyStr()))
		{
			generator.write("keysMatch", true);
		}
		else
		{
			generator.write("keysMatch", false);
			generator.write("prevKey", entry.getKeyStr());
		}

		byte[] createNaunce = Misc.createNaunce(1024);
		byte[] encrypt = Track.keys.encrypt(entry.getKey(), createNaunce);
		generator.write("naunce", Misc.format(encrypt));
		generator.writeEnd();
		generator.flush();

		byte[] decrypted = getDecryptedNaunce(input, generator);
		if (!Arrays.equals(createNaunce, decrypted))
		{
			fail("Authentication failed.", input, generator);
		}

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
				case "decrypted":    decrypted = Misc.format(input.getString());
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
		
		FileEntry file;
		MachineEntry other;
		Comment comment;
		switch (action)
		{
		case CLAIM_FILE:
			file = new FileEntry();
			if (!TrackObjectUtils.read(input, file))
			{
				fail("Claim without a file", input, output);
			}
			store.machineClaims(entry, file);
			break;
		case LIST_ALL_MACHINES:
			store.listMachines(output);
			break;
		case LIST_MY_FILES:
			store.listFiles(entry, output);
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
			store.listMachines(file, output);
			break;
		case LIST_TRACKERS:
			break;
		case LOSE_FILE:
			file = new FileEntry();
			if (!TrackObjectUtils.read(input, file))
			{
				fail("Removed without a file", input, output);
			}
			store.machineLost(entry, file);
			break;
		case POST_COMMENT:
			comment = new Comment();
			if (!TrackObjectUtils.read(input, comment))
			{
				fail("Post without a comment.", input, output);
			}
			other = new MachineEntry();
			if (!TrackObjectUtils.read(input, other))
			{
				fail("Post without a machine.", input, output);
			}
			store.postComment(comment);
			break;
		case POST_MACHINE:
			// already done...
			break;
		default:
			fail("Unkown TrackAction: " + action, input, output);
		}
	}
	
	private void fail(String message, JsonParser input, JsonGenerator generator) throws TrackerException
	{
		generator.writeStartObject();
		generator.write("errorMsg", message);
		generator.writeEnd();
		generator.flush();

		final Thread t = Thread.currentThread();
		class EnsureClosed extends TimerTask
		{
			boolean closed = false;
			@Override
			public void run()
			{
				if (closed) return;
				generator.close();
			}
		}
		
		EnsureClosed task = new EnsureClosed();
		Track.timer.schedule(task, 10 * 1000);
		waitForClient(input);
		task.closed = true;
		throw new TrackerException(message);
	}
}
