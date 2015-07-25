package org.cnv.shr.cnctn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.MissingKeyException;

import de.flexiprovider.core.rijndael.RijndaelKey;
import de.flexiprovider.core.rsa.RSAPublicKey;

public class HandShake
{
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
	
	
	protected void ensureCanConnect(
			RemoteInfo info,
			String ip,
			int port,
			String expectedIdentifier)
	{
		if (info.ident == null || info.name == null || info.publicKey == null)
		{
			throw new RuntimeException("Did not find remote info.");
		}

		if (Services.blackList.contains(info.ident))
		{
			throw new RuntimeException(info.ident + " is a blacklisted machine.");
		}
		
		if (!UserActions.checkIfMachineShouldNotReplaceOld(info.ident, ip, port))
		{
			throw new RuntimeException("A different machine at " + ip + " already exists");
		}
	}

	protected static boolean authenticateTheRemote(
			JsonGenerator generator, 
			JsonParser parser,
			String expectedIdentifier,
			boolean acceptKeys)
	{
		Machine machine = DbMachines.getMachine(expectedIdentifier);
		PublicKey[] keys = DbKeys.getKeys(machine);
		
		boolean alreadyAuthenticated = keys.length <= 0 || acceptKeys;
		generator.writeStartObject();
		generator.write("authenticated", alreadyAuthenticated);
		generator.writeEnd();
		generator.flush();
		
		if (alreadyAuthenticated)
		{
			return true;
		}
		
		generator.writeStartArray("naunceTests");
		for (PublicKey key : keys)
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
				generator.flush();
				
				if (correct)
				{
					generator.writeEnd();
					generator.flush();
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


	protected static boolean authenticateToRemote(
			JsonGenerator generator, 
			JsonParser parser)
	{
		if (isAuthenticated(parser))
		{
			return true;
		}
		
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

	// if accept key anyway:
	
	
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

	
	static enum ConnectionType
	{
		JSON,
		DATA,
	}
	
	
	static class RemoteInfo
	{
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
		RijndaelKey outgoing;
		RijndaelKey incoming;
		public String ident;
	}
	
	static class EncryptionKey
	{
		RijndaelKey encryptionKey;
		
		static void sendOpenParams(
				JsonGenerator generator, 
				PublicKey remoteKey,
				RijndaelKey aesKey) throws IOException
		{
			generator.writeStartObject();
			generator.write("aesKey", Misc.format(getBytes(remoteKey, aesKey)));
			generator.writeEnd();
			generator.flush();
		}
		
		EncryptionKey(JsonParser parser, PublicKey localKey) throws ClassNotFoundException, IOException, MissingKeyException
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
					return;
				case VALUE_STRING:
					switch (key)
					{
					case "key":
						break;
					case "aesKey":
						encryptionKey = getKey(localKey, Misc.format(parser.getString()));
						break;
					}
				}
			}
		}
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
