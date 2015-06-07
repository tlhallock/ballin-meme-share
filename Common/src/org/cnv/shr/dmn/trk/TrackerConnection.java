package org.cnv.shr.dmn.trk;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

import de.flexiprovider.core.rsa.RSAPublicKey;

public abstract class TrackerConnection implements Closeable
{
	Socket socket;
	JsonParser parser;
	JsonGenerator generator;

	protected TrackerConnection(String url, int port) throws IOException
	{
		socket = new Socket(url, port);
	}

	void connect(TrackerRequest request) throws IOException
	{
		generator = TrackObjectUtils.createGenerator(socket.getOutputStream());
		generator.writeStartArray();
		
		
		
		MachineEntry local = getLocalMachine();
		local.print(generator);
		generator.flush();

		parser = TrackObjectUtils.createParser(socket.getInputStream());
		if (!parser.next().equals(JsonParser.Event.START_ARRAY))
		{
			LogWrapper.getLogger().info("Tracker connection did not start with an array.");
			socket.close();
			return;
		}
		
		authenticate();
		
		request.print(generator);
		generator.flush();
	}

	private void authenticate() throws IOException
	{
		if (!getNeedsAuthentication())
		{
			return;
		}
		generator.writeStartObject();
		byte[] naunceRequest = new byte[0];
		if (!parser.next().equals(JsonParser.Event.START_OBJECT))
		{
			throw new IOException("Expected a decrypted naunce.");
		}
		String key = null;
		RSAPublicKey publicKey = null;
		outer:
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				if (key == null) break;
				switch (key)
				{
				case "prevKey":      publicKey     = KeyPairObject.deSerializePublicKey(parser.getString()); break;
				case "naunce":       naunceRequest = Misc.format(parser.getString());                        break;
				}
				break;
			case END_OBJECT:
				break outer;
			default:
				break;
			}
		}
		
		sendDecryptedNaunce(naunceRequest, publicKey);
		generator.writeEnd();
		generator.flush();
	}

	protected abstract void sendDecryptedNaunce(byte[] naunceRequest, RSAPublicKey publicKey);

	private boolean getNeedsAuthentication() throws IOException
	{
		boolean needsAuthentication;
		if (!parser.next().equals(JsonParser.Event.START_OBJECT) ||
				!parser.next().equals(JsonParser.Event.KEY_NAME) ||
				!parser.getString().equals("need-authentication"))
				
		{
			throw new IOException("Expected a authentication notice.");
		}
		switch (parser.next())
		{
		case VALUE_FALSE: needsAuthentication = false; break;
		case VALUE_TRUE:  needsAuthentication = true;  break;
		default:
			throw new IOException("Expected a authentication notice.");
		}
		if (!parser.next().equals(JsonParser.Event.END_OBJECT))
		{
			throw new IOException("Expected a authentication notice.");
		}
		return needsAuthentication;
	}


	@Override
	public void close()
	{
		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close socket", e);
		}
//		try
//		{
//			parser.close();
//		}
//		catch (Exception e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to close parser", e);
//		}
//		try
//		{
//			reader.close();
//		}
//		catch (Exception e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to close reader", e);
//		}
	}

	protected abstract MachineEntry getLocalMachine();
}
