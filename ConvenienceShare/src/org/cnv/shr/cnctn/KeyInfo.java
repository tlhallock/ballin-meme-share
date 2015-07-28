package org.cnv.shr.cnctn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.MissingKeyException;

import de.flexiprovider.core.rsa.RSAPublicKey;

public final class KeyInfo
{
	private SecretKey aesKey;
	private byte[] initializationVector;
	
	public KeyInfo()
	{
		try
		{
			aesKey = KeyGenerator.getInstance("AES").generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "The required encryption algorithm is not present.", e);
			Services.quiter.quit();
		}
		initializationVector = new byte[16];
		new Random().nextBytes(initializationVector);
	}

	public KeyInfo(JsonParser parser, RSAPublicKey publicKey) throws ClassNotFoundException, IOException, MissingKeyException
	{
		String key = null;
		
		HandShake.expect(parser, JsonParser.Event.START_OBJECT);

		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case END_OBJECT:
				return;
			case VALUE_STRING:
				switch (key)
				{
				case "iv":
					initializationVector = Services.keyManager.decrypt(publicKey, Misc.format(parser.getString()));
					break;
				case "key":
					aesKey = getKey(parser.getString(), publicKey);
					break;
				}
				break;
			}
		}
	}
		
	public void generate(JsonGenerator generator, RSAPublicKey publicKey) throws IOException
	{
		generator.writeStartObject();
		generator.write("key", getBytes(aesKey, publicKey));
		generator.write("iv", Misc.format(Services.keyManager.encrypt(publicKey, initializationVector)));
		generator.writeEnd();
		generator.flush();
	}
	
	public Cipher createDecryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException
	{
		Cipher instance;
		try
		{
			instance = Cipher.getInstance("AES/CTR/NoPadding");
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "The required encryption algorithm is not present.", e);
			Services.quiter.quit();
			return null;
		}
    IvParameterSpec ivSpec = new IvParameterSpec(initializationVector);
		instance.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
		return instance;
	}
	
	public Cipher createEncryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException
	{
		Cipher instance;
		try
		{
			instance = Cipher.getInstance("AES/CTR/NoPadding");
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "The required encryption algorithm is not present.", e);
			Services.quiter.quit();
			return null;
		}
    IvParameterSpec ivSpec = new IvParameterSpec(initializationVector);
		instance.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
		return instance;
	}
	
	private static String getBytes(SecretKey key, RSAPublicKey pKey) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
		objectOutputStream.writeObject(key);
		return Misc.format(Services.keyManager.encrypt(pKey, output.toByteArray()));
	}
	private static SecretKey getKey(String key, RSAPublicKey pKey) throws ClassNotFoundException, IOException, MissingKeyException
	{
		ByteArrayInputStream input = new ByteArrayInputStream(Services.keyManager.decrypt(pKey, Misc.format(key)));
		ObjectInputStream objectOutputStream = new ObjectInputStream(input);
		return (SecretKey) objectOutputStream.readObject();
	}
}