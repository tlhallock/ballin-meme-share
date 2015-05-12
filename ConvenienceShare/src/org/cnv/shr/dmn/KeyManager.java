package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.InitiateAuthentication;
import org.cnv.shr.util.Misc;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.flexiprovider.api.keys.KeySpec;
import de.flexiprovider.common.math.FlexiBigInt;
import de.flexiprovider.core.FlexiCoreProvider;
import de.flexiprovider.core.rsa.RSAPrivateCrtKey;
import de.flexiprovider.core.rsa.RSAPrivateCrtKeySpec;
import de.flexiprovider.core.rsa.RSAPrivateKey;
import de.flexiprovider.core.rsa.RSAPrivateKeySpec;
import de.flexiprovider.core.rsa.RSAPublicKey;
import de.flexiprovider.pki.PKCS8EncodedKeySpec;
import de.flexiprovider.pki.X509EncodedKeySpec;

public class KeyManager
{
	private static FlexiCoreProvider provider = new FlexiCoreProvider();
	
	HashMap<String, KeyPairObject> keys = new HashMap<>();
	File keysFile;
	
	
	public KeyManager(File keysFile)
	{
		this.keysFile = keysFile;
		Security.addProvider(provider);
	}
	
	public void writeKeys() throws IOException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(keysFile)))
		{
			ps.println(keys.size());
			for (KeyPairObject pair : keys.values())
			{
				pair.write(ps);
			}
		}
	}
	
	public void readKeys() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IOException, CertificateEncodingException, InvalidKeySpecException, ClassNotFoundException
	{
		try (Scanner scanner = new Scanner(new FileReader(keysFile)))
		{
			int length = scanner.nextInt();
			for (int i=0;i<length;i++)
			{
				KeyPairObject keyPairObject = new KeyPairObject(scanner);
				keys.put(keyPairObject.hash(), keyPairObject);
			}
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
		kpg.initialize(1024 /*Services.settings.keySize.get()*/);
		KeyPairObject keyPairObject = new KeyPairObject(kpg);
		keys.put(keyPairObject.hash(), keyPairObject);
		writeKeys();
	}

	public String[] getKeys()
	{
		return new String[0];
	}

	public PublicKey getPublicKey()
	{
		return keys.get(0).publicKey;
	}
	public PrivateKey getPrivateKey()
	{
		return keys.get(0).privateKey;
	}

	public boolean containsKey(PublicKey destinationPublicKey)
	{
		if (destinationPublicKey == null)
		{
			return false;
		}
		return false;
	}
	
//	public boolean confirmPendingNaunce(PublicKey publicKey, byte[] original, byte[] encrtypedNaunce2) throws IOException
//	{
//		return Arrays.equals(original, encrtypedNaunce2);
//		try
//		{
//			PrivateKey privateKey = getPrivateKey(publicKey);
//			Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore");
//			cipher2.init(Cipher.DECRYPT_MODE, privateKey);
//			
//			ByteOutputStream output = new ByteOutputStream();
//			try (ByteInputStream input = new ByteInputStream(encrtypedNaunce2, 0, encrtypedNaunce2.length);)
//			{
//				Misc.copy(new CipherInputStream(input, cipher2), output);
//			}
//			return Arrays.equals(original, output.getBytes());
//		}
//		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
//		{
//			e.printStackTrace();
//			Main.quit();
//			return false;
//		}
//	}
	
	public byte[] decryptNaunce(PublicKey pKey, byte[] encrypted) throws IOException
	{
		try
		{
			PrivateKey privateKey = getPrivateKey(pKey);
			Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore");
			cipher2.init(Cipher.DECRYPT_MODE, privateKey);
			
			ByteOutputStream output = new ByteOutputStream();
			try (ByteInputStream input = new ByteInputStream(encrypted, 0, encrypted.length);)
			{
				Misc.copy(new CipherInputStream(input, cipher2), output);
			}
			return output.getBytes();
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e)
		{
			e.printStackTrace();
			Main.quit();
			return new byte[0];
		}
	}
	
	public byte[] createTestNaunce(Communication c, PublicKey remoteKey) throws IOException
	{
		final byte[] original = Misc.createNaunce();
		final byte[] sentNaunce = createNaunce(remoteKey, original);
		c.addPendingNaunce(original);
		return sentNaunce;
	}
	
	public byte[] createNaunce(PublicKey remoteKey, byte[] original) throws IOException
	{
		try
		{
			Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore");
			
			cipher2.init(Cipher.ENCRYPT_MODE, remoteKey);
			
			ByteOutputStream output = new ByteOutputStream();
			try (ByteInputStream input = new ByteInputStream(original, 0, original.length);)
			{
				Misc.copy(new CipherInputStream(input, cipher2), output);
			}
			return output.getBytes();
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e)
		{
			e.printStackTrace();
			Main.quit();
			return new byte[0];
		}
	}

	private PrivateKey getPrivateKey(PublicKey publicKey)
	{
		return keys.get(hashObject(publicKey)).privateKey;
	}

	public boolean acceptKey(Machine machine, PublicKey sourcePublicKey)
	{
		return Services.application != null && Services.application.acceptKey(machine, sourcePublicKey);
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
}
