package org.cnv.shr.cnctn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.MissingKeyException;

import de.flexiprovider.core.rijndael.RijndaelKey;
import de.flexiprovider.core.rsa.RSAPublicKey;

public class HandShake
{
	protected static void expect(JsonParser parser, JsonParser.Event evnt)
	{
		Event next = parser.next();
		if (!next.equals(evnt))
		{
			LogWrapper.getLogger().warning("Expected " + evnt + ": found " + next);
		}
	}
	
	private static RijndaelKey getKey(PublicKey pKey, byte[] bytes) throws IOException, ClassNotFoundException, MissingKeyException
	{
		try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(
				Services.keyManager.decrypt(pKey, bytes)));)
		{
			return (RijndaelKey) objectInputStream.readObject();
		}
	}
	
	private static byte[] getBytes(PublicKey key, RijndaelKey aesKey) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);)
		{
			objectOutputStream.writeObject(aesKey);
		}
		return Services.keyManager.encrypt(key, byteArrayOutputStream.toByteArray());
	}

	public static boolean verifyMachine(
			String ident,
			String ip,
			int port,
			String name,
			RSAPublicKey publicKey)
	{
		LogWrapper.getLogger().info("Remote identifer is " + ident);
		if (ident == null || Services.blackList.contains(ident))
		{
			LogWrapper.getLogger().info("This machine is blacklisted.");
			return false;
		}

		Machine findAnExistingMachine = DbMachines.findAnExistingMachine(ip, port);
		if (findAnExistingMachine == null
				|| findAnExistingMachine.getIdentifier().equals(ident))
		{
			return true;
		}
		
		LogWrapper.getLogger().info("The machine at this address matches another machine at this address with a different identifer.\nDid it change identifiers?");
		
		UserMessage.addChangeIdentifierMessage(
				findAnExistingMachine.getIdentifier(),
				ident,
				ip,
				port,
				name,
				publicKey);
		return false;
	}
	
	protected static final void sendInfo(JsonGenerator generator, RSAPublicKey key, String reason)
	{
		generator.writeStartObject();
		generator.write("ident", Services.localMachine.getIdentifier());
		generator.write("port", Services.localMachine.getPort());
		generator.write("name", Services.localMachine.getName());
		generator.write("publicKey", KeyPairObject.serialize(key));
		if (reason != null)
			generator.write("reason", reason);
		generator.writeEnd();
		generator.flush();
	}
	
	protected static final RemoteInfo readInfo(JsonParser parser)
	{
		RemoteInfo returnValue = new RemoteInfo();
		String key = null;

		expect(parser, JsonParser.Event.START_OBJECT);
		
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
				case "reason":
					returnValue.reason = parser.getString();
					break;
				default:
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_TRUE:
				if (key.equals("showGui"))
				{
					returnValue.showGui = true;
				}
				break;
			case VALUE_NUMBER:
				switch (key)
				{
				case "port":
					returnValue.port = parser.getInt();
					break;
				default:
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
		return returnValue;
	}
	
	protected static boolean authenticateTheRemote(
			JsonGenerator generator, 
			JsonParser parser,
			String expectedIdentifier,
			boolean acceptKeys)
	{
		LogWrapper.getLogger().info("Authenticating the remote.");
		Machine machine = DbMachines.getMachine(expectedIdentifier);
		PublicKey[] keys = DbKeys.getKeys(machine);
		
		boolean alreadyAuthenticated = keys.length <= 0 || acceptKeys;
		
		sendAuthentication(generator, alreadyAuthenticated);
		
		if (alreadyAuthenticated)
		{
			LogWrapper.getLogger().info("We either have no keys for the remote or are accepting all. Accepting new key...");
			return true;
		}
		
		int count = 0;
		
		generator.writeStartArray();
		for (PublicKey key : keys)
		{
			byte[] naunce = Misc.createNaunce(Services.settings.minNaunce.get());
			try
			{
				LogWrapper.getLogger().info("Creating naunce with key #" + ++count);
				byte[] encrypted = Services.keyManager.encrypt(key, naunce);

				generator.writeStartObject();
				generator.write("publicKey", KeyPairObject.serialize(key));
				generator.write("naunce", Misc.format(encrypted));
				generator.writeEnd();
				generator.flush();
				
				String decrypted = getNaunceResponse(parser);
				boolean correct = decrypted != null && decrypted.equals(Misc.format(naunce));

				sendAuthentication(generator, correct);
				
				if (correct)
				{
					LogWrapper.getLogger().info("The remote was able to authenticate with this key.");
					generator.writeEnd();
					generator.flush();
					return true;
				}
				LogWrapper.getLogger().info("Remote failed to authenticate with this key.");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		LogWrapper.getLogger().info("The failed to authenticate with any keys we have. Terminating connection, and creating a message to add the new key.");
		
		generator.writeEnd();
		return false;
	}
	protected static boolean authenticateToRemote(
			JsonGenerator generator, 
			JsonParser parser)
	{
		LogWrapper.getLogger().info("Authenticating ourselves...");
		if (isAuthenticated(parser))
		{
			LogWrapper.getLogger().info("Remote does not need to authenticate us.");
			return true;
		}

		expect(parser, JsonParser.Event.START_ARRAY);
		
		int count = 0;
		
		outer:
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case END_ARRAY:
				break outer;
			case START_OBJECT:
				LogWrapper.getLogger().info("Trying the remote's key #" + ++count);
				new NaunceTest(parser).respond(generator);
				if (isAuthenticated(parser))
				{
					LogWrapper.getLogger().info("Successful.");
					expect(parser, JsonParser.Event.END_ARRAY);
					return true;
				}
				LogWrapper.getLogger().info("Fail.");
				break;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}

		LogWrapper.getLogger().info("Failure: we failed to authenticate with any of the keys the remote has for us.");
		expect(parser, JsonParser.Event.END_ARRAY);
	
		return false;
	}

	private static void sendAuthentication(JsonGenerator generator, boolean alreadyAuthenticated)
	{
		generator.writeStartObject();
		generator.write("authenticated", alreadyAuthenticated);
		generator.writeEnd();
		generator.flush();
	}

	private static String getNaunceResponse(JsonParser parser)
	{
		boolean hasKey = false;
		String returnValue = null;
		String key = null;
		
		expect(parser, JsonParser.Event.START_OBJECT);
		
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
			case VALUE_TRUE:
				if (key.equals("hasKey"))
					hasKey = true;
				break;
			case VALUE_FALSE:
				if (key.equals("hasKey"))
					hasKey = false;
				break;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
		return returnValue;
	}
	
	private static boolean isAuthenticated(JsonParser parser)
	{
		expect(parser, JsonParser.Event.START_OBJECT);
		
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
			case VALUE_FALSE:
				break;
			case END_OBJECT:
				return success;
			default:
				LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
		return false;
	}

	
	static enum ConnectionType
	{
		JSON,
		DATA,
	}
	
	
	static class RemoteInfo
	{
		public boolean showGui;
		public String reason;
		String ident;
		RSAPublicKey publicKey;
		String name;
		int port;
	}
	
	static class HandShakeResults
	{
		int port;
		PublicKey remoteKey;
		RSAPublicKey localKey;
		KeyInfo outgoing;
		KeyInfo incoming;
		public String ident;
	}
	
//	static class EncryptionKey
//	{
//		RijndaelKey encryptionKey;
//		
//		static void sendOpenParams(
//				JsonGenerator generator, 
//				PublicKey remoteKey,
//				RijndaelKey aesKey) throws IOException
//		{
//			generator.writeStartObject();
//			generator.write("aesKey", Misc.format(getBytes(remoteKey, aesKey)));
//			generator.writeEnd();
//			generator.flush();
//		}
//		
//		EncryptionKey(JsonParser parser, PublicKey localKey) throws ClassNotFoundException, IOException, MissingKeyException
//		{
//			expect(parser, JsonParser.Event.START_OBJECT);
//			
//			String key = null;
//			while (parser.hasNext())
//			{
//				JsonParser.Event e = parser.next();
//				switch (e)
//				{
//				case KEY_NAME:
//					key = parser.getString();
//					break;
//				case END_OBJECT:
//					return;
//				case VALUE_STRING:
//					switch (key)
//					{
//					case "aesKey":
//						encryptionKey = getKey(localKey, Misc.format(parser.getString()));
//						break;
//					}
//				}
//			}
//		}
//	}

	static class NaunceTest
	{
		String naunce;
		RSAPublicKey publicKey;
		
		public NaunceTest(JsonParser parser)
		{
			String key = null;
			
//			expect(parser, JsonParser.Event.START_OBJECT);
			
			while (parser.hasNext())
			{
				JsonParser.Event e = parser.next();
				switch (e)
				{
				case KEY_NAME:
					key = parser.getString();
					break;
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
