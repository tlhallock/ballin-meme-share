package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.swing.JFrame;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.MissingKeyException;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class HandShake
{
	public static void handleHandShake(Socket socket)
	{
		try (JsonParser parser       = TrackObjectUtils.createParser(   new SnappyFramedInputStream(socket.getInputStream(), true));
				 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(socket.getOutputStream()));)
		{
			generator.writeStartArray();
			
			
			generator.writeEnd();
		}
		catch (UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static final void sendInfo(JsonGenerator generator, RSAPublicKey key)
	{
		generator.writeStartObject();
		generator.write("ident", Services.localMachine.getIdentifier());
		generator.write("port", Services.localMachine.getPort());
		generator.write("name", Services.localMachine.getName());
		generator.write("publicKey", KeyPairObject.serialize(key));
		generator.writeEnd();
	}
	
	private static final MachineUpdateInfo getRemoteInfo(JsonParser parser, String ip, int port)
	{
		MachineUpdateInfo returnValue = new MachineUpdateInfo();
		String key = null;
		outer:
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case END_OBJECT:
				break outer;
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				switch (key)
				{
				case "ident":
					returnValue.ident = parser.getString();
					break;
				case "publicKey":
					returnValue.publicKey = KeyPairObject.deSerializePublicKey(parser.getString());
					break;
				case "name":
					returnValue.name = parser.getString();
					break;
				default:
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
			case VALUE_NUMBER:
				switch (key)
				{
				case "port":
					break;
				default:
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}

		if (returnValue.ident == null || returnValue.name == null || returnValue.publicKey == null)
		{
			throw new RuntimeException("Did not find remote info.");
		}

		if (Services.blackList.contains(returnValue.ident))
		{
			throw new RuntimeException(returnValue.ident + " is a blacklisted machine.");
		}
		
		if (!UserActions.checkIfMachineShouldNotReplaceOld(returnValue.ident, ip, port))
		{
			throw new RuntimeException("A different machine at " + ip + " already exists");
		}
		
		Machine existingMachine = DbMachines.getMachine(returnValue.ident);
		
		
		return returnValue;
	}

	private static final boolean authenticateTheRemote(
			JsonGenerator generator, 
			JsonParser parser,
			String identifier)
	{
		Machine machine = DbMachines.getMachine(identifier);
		
		generator.writeStartArray("naunceTests");
		for (PublicKey key : DbKeys.getKeys(machine))
		{
			byte[] naunce = Misc.createNaunce(Services.settings.minNaunce.get());
			try
			{
				byte[] encrypted = Services.keyManager.encrypt(key, naunce);

				generator.writeStartObject();
				generator.write("publicKey", KeyPairObject.serialize(key));
				generator.write("naunce", Misc.format(encrypted));
				generator.writeEnd();
				generator.flush();
				
				String decrypted = getNaunceResponse(parser);
				boolean correct = decrypted != null && decrypted.equals(Misc.format(naunce));
				
				generator.writeStartObject();
				generator.write("authenticated", correct);
				generator.writeEnd();
				
				if (correct)
				{
					generator.writeEnd();
					return true;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		generator.writeEnd();
		return false;
	}

	private static String getNaunceResponse(JsonParser parser)
	{
		boolean hasKey = false;
		String returnValue = null;
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				if (key.equals("decrypted"))
					returnValue = parser.getString();
				break;
			case END_OBJECT:
				return returnValue;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
		return returnValue;
	}


	private static final boolean authenticateToRemote(
			JsonGenerator generator, 
			JsonParser parser,
			String identifier)
	{
		outer:
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case END_ARRAY:
				break outer;
			case START_OBJECT:
				new NaunceTest(parser).respond(generator);
				if (isAuthenticated(parser))
				{
					return true;
				}
				break;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
		return false;
	}
	
	private static boolean isAuthenticated(JsonParser parser)
	{
		boolean success = false;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				if (!parser.getString().equals("authenticated"))
					throw new RuntimeException("Excpected to see confirmation that we are authenticated.");
				break;
			case VALUE_TRUE:
				success = true;
				break;
			case END_OBJECT:
				return success;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
		return false;
	}
	
	
	public static HandShakeResults initiateHandShake(HandShakeParams params)
	{
		HandShakeResults results = new HandShakeResults();
		try (Socket socket           = new Socket(params.ip, params.port);
				 JsonParser parser       = TrackObjectUtils.createParser(   new SnappyFramedInputStream(socket.getInputStream(), true));
				 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(socket.getOutputStream()));)
		{
			generator.writeStartArray();
			
			results.localKey = Services.keyManager.getPublicKey();
			sendInfo(generator, results.localKey);
			authenticateToRemote(generator, parser);
			
			results.remoteKey = params.remoteKey;
			if (params.remoteKey != null)
			{
				final byte[] original = Misc.createNaunce(Services.settings.minNaunce.get());
				final byte[] sentNaunce = Services.keyManager.encrypt(params.remoteKey, original);
			}
			
			
			
			generator.writeEnd();
			

		}
		catch (UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void authenticateToRemote(JsonGenerator generator, JsonParser parser)
	{
		
	}

	static class MachineUpdateInfo
	{
		String ident;
		RSAPublicKey publicKey;
		String name;
	}
	
	static class HandShakeParams
	{
		JFrame origin;
		String identifier;
		String ip;
		int port;
		PublicKey remoteKey;
		boolean acceptAllKeys;
		String reason;
	}
	
	
	static class HandShakeResults
	{
		int port;
		PublicKey remoteKey;
		RSAPublicKey localKey;
		boolean data;
		boolean successful;
	}

	static class NaunceTest
	{
		String naunce;
		RSAPublicKey publicKey;
		
		public NaunceTest(JsonParser parser)
		{
			String key = null;
			while (parser.hasNext())
			{
				JsonParser.Event e = parser.next();
				switch (e)
				{
				case KEY_NAME:
					key = parser.getString();
				case END_OBJECT:
					if (naunce == null || publicKey == null) throw new RuntimeException("Bad naunce test...");
					return;
				case VALUE_STRING:
					switch (key)
					{
					case "naunce":
						naunce = parser.getString();
						break;
					case "publicKey":
						publicKey = KeyPairObject.deSerializePublicKey(parser.getString());
						break;
					}
				}
			}
		}
		
		public void respond(JsonGenerator generator)
		{
			generator.writeStartObject();
			try
			{
				byte[] decrypt = Services.keyManager.decrypt(publicKey, Misc.format(naunce));
				generator.write("decrypted", Misc.format(decrypt));
				generator.write("hasKey", true);
			}
			catch (MissingKeyException | IOException e)
			{
				generator.write("hasKey", false);
				e.printStackTrace();
			}
			generator.writeEnd();
			generator.flush();
		}
	}
}
