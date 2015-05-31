package org.cnv.shr.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import de.flexiprovider.core.FlexiCoreProvider;
import de.flexiprovider.core.rijndael.RijndaelKey;

public class KeysService
{
	private HashMap<String, KeyPairObject> keys = new HashMap<>();
	private KeyPairObject primaryKey;
	private PublicKey codeUpdateKey;

	private HashSet<String> pendingAuthenticationRequests = new HashSet<>();
	
	private static final int MAX_CIPHER_LENGTH = 117;
	
	public KeysService()
	{
		Security.addProvider(new FlexiCoreProvider());
	}
	
	public void writeKeys(Path f) throws IOException
	{
		Misc.ensureDirectory(f, true);
		try (PrintStream ps = new PrintStream(Files.newOutputStream(f)))
		{
			ps.println(keys.size());
			for (KeyPairObject pair : keys.values())
			{
				pair.write(ps);
				ps.println();
			}
			ps.println(pendingAuthenticationRequests.size());
			for (String url : pendingAuthenticationRequests)
			{
				ps.println(url);
			}
		}
	}
	
	public void readKeys(Path f, int keyLength) throws IOException
	{
		try (Scanner scanner = new Scanner(f))
		{
			int length = scanner.nextInt();
			for (int i = 0; i < length; i++)
			{
				KeyPairObject keyPairObject = new KeyPairObject(scanner);
				if (primaryKey == null || keyPairObject.timeStamp > primaryKey.timeStamp)
				{
					primaryKey = keyPairObject;
				}
				keys.put(keyPairObject.hash(), keyPairObject);
			}
			length = scanner.nextInt();
			for (int i = 0; i < length; i++)
			{
				pendingAuthenticationRequests.add(scanner.nextLine());
			}
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().info("Unable to read previous keys.");
			writeKeys(f);
		}
		generateNecessaryKeys(f, keyLength);
	}
	
	public void generateNecessaryKeys(Path f, int length) throws IOException
	{
		if (!keys.isEmpty() || length < 0)
		{
			return;
		}
		
		try
		{
			createAnotherKey(f, length);
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException e)
		{
			LogWrapper.getLogger().info("Unable to generate keys.");
			e.printStackTrace();
		}
	}

	public void createAnotherKey(Path f, int length) throws NoSuchAlgorithmException, NoSuchProviderException, IOException
	{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "FlexiCore");
		kpg.initialize(length);
		KeyPairObject keyPairObject = new KeyPairObject(kpg);
		keys.put(keyPairObject.hash(), keyPairObject);
		primaryKey = keyPairObject;
		writeKeys(f);
	}

	public String[] getKeys()
	{
		return new String[0];
	}

	public PublicKey getPublicKey()
	{
		return primaryKey.publicKey;
	}
	
	public PrivateKey getPrivateKey()
	{
		return primaryKey.privateKey;
	}

	public boolean containsKey(PublicKey destinationPublicKey)
	{
		if (destinationPublicKey == null)
		{
			return false;
		}

		return keys.get(KeyPairObject.hashObject(destinationPublicKey)) != null;
	}
	
	public void addPendingAuthentication(Path f, String url)
	{
		pendingAuthenticationRequests.add(url);
		try
		{
			writeKeys(f);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write keys.", e);
		}
	}

	private static byte[] copyOf(byte[] original, int start, int end)
	{
		byte[] returnValue = new byte[end - start];
		System.arraycopy(original, start, returnValue, 0, end - start);
		return returnValue;
	}
	
	public byte[] encrypt(PublicKey pKey, byte[] original)
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (InputStream input = new ByteArrayInputStream(original))
		{
			int offset = 0;
			while (offset < original.length)
			{
				int end = Math.min(original.length, offset + MAX_CIPHER_LENGTH);
				Misc.writeBytes(encryptChunk(pKey, copyOf(original, offset, end)), output);
				offset = end;
			}
			
			Misc.writeBytes(new byte[0], output);

			return output.toByteArray();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to encrypt.", e);
			return new byte[0];
		}
	}

	public byte[] decrypt(PublicKey pKey, byte[] encrypted)
	{
		if (pKey == null) return new byte[0];
		return decrypt(getPrivateKey(pKey), encrypted);
	}

	public byte[] decrypt(PrivateKey privateKey, byte[] encrypted)
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (privateKey == null) return new byte[0];
		
		byte[] encryptedChunk;
		try (InputStream input = new ByteArrayInputStream(encrypted))
		{
			while ((encryptedChunk = Misc.readBytes(input)).length > 0)
			{
				output.write(decryptChunk(privateKey, encryptedChunk));
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to decrypt", e);
		}
		return output.toByteArray();
	}

	public byte[] encryptChunk(PublicKey pKey, byte[] original)
	{
		try
		{
			Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore");
			cipher2.init(Cipher.ENCRYPT_MODE, pKey);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (InputStream input = new ByteArrayInputStream(original, 0, original.length);)
			{
				Misc.copy(new CipherInputStream(input, cipher2), output);
			}
			byte[] byteArray = output.toByteArray();
			

			if (LogWrapper.getLogger().isLoggable(Level.FINE))
			{
				LogWrapper.getLogger().fine("Encrypted");
				LogWrapper.getLogger().fine(Misc.format(original));
				LogWrapper.getLogger().fine("to");
				LogWrapper.getLogger().fine(Misc.format(byteArray));
				LogWrapper.getLogger().fine("with");
				LogWrapper.getLogger().fine(Misc.format(pKey.getEncoded()));
			}

			return byteArray;
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "WTH?", e);
			System.exit(-1);
			return new byte[0];
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Quiting:", e);
			System.exit(-1);
			return new byte[0];
		}
	}

	public byte[] decryptChunk(PrivateKey privateKey, byte[] encrypted)
	{
		try
		{
			Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore");
			cipher2.init(Cipher.DECRYPT_MODE, privateKey);

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (ByteArrayInputStream input = new ByteArrayInputStream(encrypted, 0, encrypted.length);)
			{
				Misc.copy(new CipherInputStream(input, cipher2), output);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to copy stream", e);
			}
			byte[] bytes = output.toByteArray();

			if (LogWrapper.getLogger().isLoggable(Level.FINE))
			{
				LogWrapper.getLogger().fine("Decrypted");
				LogWrapper.getLogger().fine(Misc.format(encrypted));
				LogWrapper.getLogger().fine("to");
				LogWrapper.getLogger().fine(Misc.format(bytes));
				LogWrapper.getLogger().fine("with");
				LogWrapper.getLogger().fine(Misc.format(privateKey.getEncoded()));
			}
 
			return bytes;
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Quiting:", e);
			System.exit(-1);
			return new byte[0];
		}
	}

	public PrivateKey getPrivateKey(PublicKey publicKey)
	{
		return keys.get(KeyPairObject.hashObject(publicKey)).privateKey;
	}
	
	public static RijndaelKey createAesKey()
	{
		try
		{
			return (RijndaelKey) KeyGenerator.getInstance("AES", "FlexiCore").generateKey();
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "No provider", e);
			System.exit(-1);
			return null;
		}
	}

	public HashSet<String> getPendingAuthenticationRequests()
	{
		return pendingAuthenticationRequests;
	}
}
