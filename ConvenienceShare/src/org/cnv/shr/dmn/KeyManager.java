package org.cnv.shr.dmn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.cnctn.Authenticator;
import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import de.flexiprovider.common.math.FlexiBigInt;
import de.flexiprovider.core.FlexiCoreProvider;
import de.flexiprovider.core.rijndael.RijndaelKey;
import de.flexiprovider.core.rsa.RSAPrivateCrtKey;
import de.flexiprovider.core.rsa.RSAPublicKey;

public class KeyManager
{
	HashMap<String, KeyPairObject> keys = new HashMap<>();
	KeyPairObject primaryKey;
	File keysFile;

	private HashSet<String> pendingAuthenticationRequests = new HashSet<>();
	
	private static final int MAX_CIPHER_LENGTH = 117;
	
	public KeyManager(File keysFile)
	{
		this.keysFile = keysFile;
		Security.addProvider(new FlexiCoreProvider());
	}
	
	public void writeKeys() throws IOException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(keysFile)))
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
	
	public void readKeys() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IOException, CertificateEncodingException, InvalidKeySpecException, ClassNotFoundException
	{
		try (Scanner scanner = new Scanner(new FileReader(keysFile)))
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
			Services.logger.println("Unable to read previous keys.");
			writeKeys();
		}
		generateNecessaryKeys();
	}
	
	public void generateNecessaryKeys() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IOException
	{
		if (!keys.isEmpty())
		{
			return;
		}
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "FlexiCore");
		kpg.initialize(Services.settings.keySize.get());
		KeyPairObject keyPairObject = new KeyPairObject(kpg);
		keys.put(keyPairObject.hash(), keyPairObject);
		primaryKey = keyPairObject;
		writeKeys();
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

		return keys.get(hashObject(destinationPublicKey)) != null;
	}
	
	public void addPendingAuthentication(String url)
	{
		pendingAuthenticationRequests.add(url);
		try
		{
			writeKeys();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
	}
	
	public void attemptAuthentications() throws IOException
	{
		outer: for (;;)
		{
			for (String url : pendingAuthenticationRequests)
			{
				Communication openConnection;
				try
				{
					openConnection = Services.networkManager.openConnection(url, true);
					if (openConnection != null)
					{
						openConnection.send(new FindMachines());
						openConnection.send(new MachineFound());
						pendingAuthenticationRequests.remove(url);
						writeKeys();
						continue outer;
					}
				}
				catch (Exception e)
				{
					Services.logger.print(e);
					continue;
				}
			}
		}
	}

	
	public byte[] createTestNaunce(Authenticator authentication, PublicKey remoteKey) throws IOException
	{
		if (remoteKey == null)
		{
			return new byte[0];
		}
		final byte[] original = Misc.createNaunce();
		final byte[] sentNaunce = encrypt(remoteKey, original);
		authentication.addPendingNaunce(original);
		return sentNaunce;
	}

	private static byte[] copyOf(byte[] original, int start, int end)
	{
		byte[] returnValue = new byte[end - start];
		System.arraycopy(original, start, returnValue, 0, end - start);
		return returnValue;
	}
	
	public byte[] encrypt(PublicKey pKey, byte[] original)
	{
		try
		{
			ByteListBuffer buffer = new ByteListBuffer();

			int offset = 0;
			while (offset < original.length)
			{
				int end = Math.min(original.length, offset + MAX_CIPHER_LENGTH);
				buffer.appendVarByteArray(encryptChunk(pKey, copyOf(original, offset, end)));
			}

			return buffer.getBytes();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new byte[0];
		}
	}

	public byte[] decrypt(PublicKey pKey, byte[] encrypted)
	{
		ByteListBuffer buffer = new ByteListBuffer();
		if (pKey == null) return buffer.getBytes();
		PrivateKey privateKey = getPrivateKey(pKey);
		if (privateKey == null) return buffer.getBytes();
		
		ByteArrayInputStream in = new ByteArrayInputStream(encrypted);
		byte[] encryptedChunk;
		try
		{
			while ((encryptedChunk = ByteReader.readVarByteArray(in)) != null)
			{
				buffer.append(decryptChunk(privateKey, encryptedChunk));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return buffer.getBytes();
	}

	public byte[] encryptChunk(PublicKey pKey, byte[] original)
	{
		try
		{
			Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore");
			cipher2.init(Cipher.ENCRYPT_MODE, pKey);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (InputStream input = new ByteInputStream(original, 0, original.length);)
			{
				Misc.copy(new CipherInputStream(input, cipher2), output);
			}
			byte[] byteArray = output.toByteArray();
			
//			System.out.println("Encrypted");
//			System.out.println(Misc.format(original));
//			System.out.println("to");
//			System.out.println(Misc.format(byteArray));
//			System.out.println("with");
//			System.out.println(Misc.format(pKey.getEncoded()));

			return byteArray;
		}
		catch (IOException e)
		{
			Services.logger.println("WTH?");
			Services.logger.print(e);
			Services.quiter.quit();
			return new byte[0];
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			Services.logger.println("Quiting:");
			Services.logger.print(e);
			Services.quiter.quit();
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
			try (ByteInputStream input = new ByteInputStream(encrypted, 0, encrypted.length);)
			{
				Misc.copy(new CipherInputStream(input, cipher2), output);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			byte[] bytes = output.toByteArray();

//			 System.out.println("Decrypted");
//			 System.out.println(Misc.format(encrypted));
//			 System.out.println("to");
//			 System.out.println(Misc.format(bytes));
//			 System.out.println("with");
//			 System.out.println(Misc.format(pKey.getEncoded()));

			return bytes;
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			Services.logger.println("Quiting:");
			Services.logger.print(e);
			Services.quiter.quit();
			return new byte[0];
		}
	}

	PrivateKey getPrivateKey(PublicKey publicKey)
	{
		return keys.get(hashObject(publicKey)).privateKey;
	}

	private static String hashObject(PublicKey publicKey)
	{
		return Misc.format(publicKey.getEncoded());
	}
	
	private static class KeyPairObject
	{
		private de.flexiprovider.core.rsa.RSAPublicKey publicKey;
		private de.flexiprovider.core.rsa.RSAPrivateCrtKey privateKey;
		private long timeStamp;
		
		public KeyPairObject(KeyPairGenerator kpg)
		{
			KeyPair pair = kpg.generateKeyPair();
			privateKey = (RSAPrivateCrtKey) pair.getPrivate();
			publicKey = (RSAPublicKey) pair.getPublic();
			timeStamp = System.currentTimeMillis();
		}
		
		public KeyPairObject(Scanner scanner) throws InvalidKeySpecException
		{
			FlexiBigInt privn         = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt prive         = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt privd         = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt privp         = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt privq         = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt privdP        = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt privdQ        = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt privcrtCoeff  = new FlexiBigInt(Misc.format(scanner.next()));
			
			FlexiBigInt publn         = new FlexiBigInt(Misc.format(scanner.next()));
			FlexiBigInt puble         = new FlexiBigInt(Misc.format(scanner.next()));
			timeStamp  = scanner.nextLong();
			
			privateKey = new de.flexiprovider.core.rsa.RSAPrivateCrtKey(
					privn       ,
					prive       ,
					privd       ,
					privp       ,
					privq       ,
					privdP      ,
					privdQ      ,
					privcrtCoeff);
			publicKey  = new de.flexiprovider.core.rsa.RSAPublicKey(publn, puble);
		}
		
		public void write(PrintStream output) throws IOException
		{
			output.print(Misc.format(privateKey.getN()       .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getE()       .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getD()       .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getP()       .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getQ()       .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getDp()      .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getDq()      .toByteArray()) + " ");
			output.print(Misc.format(privateKey.getCRTCoeff().toByteArray()) + " ");
			
			output.print(Misc.format( publicKey.getN().toByteArray()) + " ");
			output.print(Misc.format( publicKey.getE().toByteArray()) + " ");
			output.print(timeStamp);
		}
		
		public String hash()
		{
			return hashObject(publicKey);
		}
	}

	public RijndaelKey createAesKey()
	{
		try
		{
			return (RijndaelKey) KeyGenerator.getInstance("AES", "FlexiCore").generateKey();
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException e)
		{
			Services.logger.print(e);
			Services.quiter.quit();
			return null;
		}
	}
}
