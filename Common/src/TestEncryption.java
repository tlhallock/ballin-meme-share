import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.cnv.shr.util.FlushableEncryptionStreams2;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.TransferStream;
import org.junit.Assert;
import org.junit.Test;


public class TestEncryption
{

	@Test
	public void simple() throws Exception
	{
		int bufferSize = 32;
		byte[][] testStrings = new byte[][] { "".getBytes(), "short".getBytes(), "really, really, really, really, really, really, really, reall long".getBytes() };
		performTest(bufferSize, testStrings);
	}
	
	private static final byte[][] DUMMY = new byte[0][0];
	
	@Test
	public void testFuzzy() throws Exception
	{
		Random random = new Random(50);
		for (int i = 0; i < 1000; i++)
		{
			System.out.println("Test 1");
			int total = 1024 * 1024;
			int bufferSize = random.nextInt(1024 * 1024);
//			bufferSize = 8192;
			LinkedList<byte[]> sequence = new LinkedList<>();
			for (int j = 0; /*j < sequence.size() &&*/ total > 0; j++)
			{
				int nextSize = random.nextInt(3 * bufferSize);
				total -= nextSize;
				
				byte[] bs = new byte[nextSize];
				sequence.add(bs);
				random.nextBytes(bs);
			}
			performTest(bufferSize, sequence.toArray(DUMMY));
		}
	}

	private static void performTest(int bufferSize, byte[][] testStrings) throws NoSuchAlgorithmException, IOException, InvalidKeyException, InvalidAlgorithmParameterException
	{
		SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();
		System.out.println("testing with buffersize = " + bufferSize + " and " + testStrings.length + " separate messages.");
		
		TransferStream transferStream = new TransferStream();
		
		try (OutputStream output =
						FlushableEncryptionStreams2.newFlushableEncryptionOutputStream(
								transferStream.getOutput(), aesKey, bufferSize);
				InputStream input =	
						FlushableEncryptionStreams2.newFlushableEncryptionInputStream(
								transferStream.getInput(), aesKey, bufferSize);)
		{
			
			for (byte[] originalBytes : testStrings)
			{
//				System.out.println("Writing " + originalBytes.length + " bytes.");
				output.write(originalBytes);
				output.flush();
	
				byte[] decryptedBytes = new byte[originalBytes.length];
	
				int offset = 0;
				while (offset < originalBytes.length)
				{
					int read = input.read(decryptedBytes, offset, decryptedBytes.length - offset);
					if (read < 0)
					{
						throw new RuntimeException("Hit end of stream too early...");
					}
					offset += read;
				}
				
				Assert.assertArrayEquals(originalBytes, decryptedBytes);
			}
			
			output.close();
			Assert.assertEquals(-1, input.read());
		}
	}

	private static final Cipher createCipher(SecretKey aesKey, boolean encrypt) throws InvalidKeyException, InvalidAlgorithmParameterException
	{
		try
		{
			Cipher instance = Cipher.getInstance("AES/CTR/NoPadding");
			byte[] ivBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x00, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00,
	        0x00, 0x00, 0x00, 0x00, 0x01 };

	    IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
			instance.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, aesKey, ivSpec);
//			System.out.println("block size is " + instance.getBlockSize());
//			System.out.println("iv length = " + instance.getIV().length);
			return instance;
		}
		catch (NoSuchAlgorithmException | /*NoSuchProviderException |*/ NoSuchPaddingException e1)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Unable to create aes cipher! Quiting.", e1);
			System.exit(-1);
			return null;
		}
	}
	
	public static void main(String[] args) throws Exception
	{

		SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();
		
		
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
		objectOutputStream.writeObject(aesKey);
		System.out.println(output.toByteArray());
		
		
		
		
		
		
		
		
		
		
		
		
		
		Cipher encryptCipher = createCipher(aesKey, true );
		Cipher decryptCipher = createCipher(aesKey, false);

		TransferStream transferStream = new TransferStream();

		new Thread()
		{
			public void run()
			{
				try (BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(
								new CipherInputStream(transferStream.getInput(), decryptCipher)));)
				{
					String line;
					while ((line = bufferedReader.readLine()) != null)
					{
						System.out.println(line);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}.start();

		try (OutputStream snappyOutputStream = new CipherOutputStream(transferStream.getOutput(), encryptCipher);)
		{
			for (int i = 0; i < 5; i++)
			{
				snappyOutputStream.write(("writing " + i + "\n").getBytes());
				snappyOutputStream.flush();
				Thread.sleep(1000);
			}
		}

	}
}
