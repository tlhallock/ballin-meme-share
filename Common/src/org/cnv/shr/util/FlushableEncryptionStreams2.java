package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.junit.Assert;
import org.junit.Test;

import de.flexiprovider.core.rijndael.RijndaelKey;

public class FlushableEncryptionStreams2
{
	public static final int ALIGN_ON = 16;
	public static final int BLOCK_SIZE = 8192;
	public static final int META_INFO_SIZE = 4;

	static int count = 0;
	static int sentCount = 0;
	static int readCount = 0;

	public static OutputStream newFlushableEncryptionOutputStream(OutputStream delegate, RijndaelKey aesKey) throws InvalidKeyException, InvalidAlgorithmParameterException
	{
		return newFlushableEncryptionOutputStream(delegate, aesKey, BLOCK_SIZE);
	}
	
	private static int nextLarger(int length)
	{
		int mod = length % ALIGN_ON;
		if (mod == 0)
			return length;
		return length + ALIGN_ON - mod;
	}
	
	public static OutputStream newFlushableEncryptionOutputStream(OutputStream delegate, SecretKey aesKey, int originalBufferLength) throws InvalidKeyException, InvalidAlgorithmParameterException
	{
		OutputStream returnValue = new OutputStream()
		{
			Cipher cipher = createCipher(aesKey, true);
			byte[] inBuffer  = new byte[nextLarger(originalBufferLength)];
			byte[] outBuffer = new byte[cipher.getOutputSize(inBuffer.length)];
			int inOffset = META_INFO_SIZE;
			Random random = new Random();
			
			
//			{
//				byte[] iv = null;
//				while (true)
//				{
//					iv = cipher.update(new byte[0]);
//					if (iv == null || iv.length == 0)
//					{
//						break;
//					}
//					delegate.write(iv);
//				}
//			}
			
			@Override
			public void write(int b) throws IOException
			{
				if (inOffset == inBuffer.length)
				{
					sendBuffer(false);
				}
				inBuffer[inOffset++] = (byte) b;
			}
			
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				while (len > 0)
				{
					int amountToWrite = inBuffer.length - inOffset;
					if (len < amountToWrite)
						amountToWrite = len;
					System.arraycopy(b, off, inBuffer, inOffset, amountToWrite);
					inOffset += amountToWrite;
					off      += amountToWrite;
					len      -= amountToWrite;
					
					if (inOffset == inBuffer.length)
						sendBuffer(false);
				}
			}
			private void sendBuffer(boolean allDone) throws IOException
			{
				// Guarantee we never send an empty frame...
				if (inOffset == META_INFO_SIZE)
				{
					// no bytes writen since last flush
					return;
				}
				int bufferSize = nextLarger(inOffset);
				
				if (inOffset < bufferSize)
				{
					nextBytesRange(random, inBuffer, inOffset, bufferSize);
				}
				
				System.out.println("Serializing " + inOffset);
				for (int i = 0; i < META_INFO_SIZE; i++)
				{
					inBuffer[i] = (byte) ((inOffset >> (i * 8)) & 0xff);
				}
				
				int written = 0;
				try
				{
					if (allDone)
						written = cipher.doFinal(inBuffer, 0, bufferSize, outBuffer, 0);
					else
						written = cipher.update(inBuffer, 0, bufferSize, outBuffer, 0);
				}
				catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e)
				{
					e.printStackTrace();
				}
				
//				System.out.println("encrypted \n" 
//						+ "[" + bufferSize + "]" + Misc.format(Arrays.copyOfRange(inBuffer, 0, bufferSize))
//						+ "\nas\n"
//						+ "[" + written + "]" + Misc.format(Arrays.copyOfRange(outBuffer, 0, written))
//						+ "\nvalid size = " + (inOffset - META_INFO_SIZE)
//						+ "\nbuffer size = " + bufferSize
//						);
				
				delegate.write(outBuffer, 0, written);
				
				System.out.println("sent "  + (sentCount++) + ": " + (inOffset - 2));
				
				inOffset = META_INFO_SIZE;
			}

			@Override
			public void flush() throws IOException
			{
				sendBuffer(false);
				delegate.flush();
			}

			@Override
			public void close() throws IOException
			{
				sendBuffer(true);
				delegate.close();
			}
		};
		return returnValue;
	}
	
	public static InputStream newFlushableEncryptionInputStream(InputStream delegate, SecretKey aesKey, int bufferLength) throws InvalidKeyException, IOException, InvalidAlgorithmParameterException
	{
		InputStream returnValue = new InputStream()
		{
			Cipher cipher = createCipher(aesKey, false);
			
			boolean closed;
			
			int validStart;
			int validEnd;
			
			int frameEnd;
			
			int decryptedEnd;
			byte[] decryptedBuffer = new byte[bufferLength];

			int encryptedStart;
			int encryptedEnd;
			byte[] encryptedBuffer = new byte[cipher.getOutputSize(decryptedBuffer.length)];
			
			private boolean readSome(int atleast, boolean failOnEnd) throws IOException
			{
//				System.out.println("Reading until " + atleast + " currently at " + decryptedEnd);
				while (decryptedEnd < atleast)
				{
					int debugStart = encryptedEnd;
//					System.out.println("Reading");
					int read = delegate.read(encryptedBuffer, encryptedEnd, encryptedBuffer.length - encryptedEnd);
					if (read < 0)
					{
						if (failOnEnd)
						{
							throw new RuntimeException("Hit of stream inside frame!");
						}
						closed = true;
						return false;
					}
					encryptedEnd += read;

//					System.out.println("Read " + read + " encrypted bytes: " + Misc.format(Arrays.copyOfRange(encryptedBuffer, debugStart, encryptedEnd)));

					debugStart = decryptedEnd;

					try
					{
						int update = 0;
//						do
//						{
							update = cipher.update(encryptedBuffer, encryptedStart, encryptedEnd - encryptedStart, decryptedBuffer, decryptedEnd);
//							System.out.println("Decrypted " + update + " of the bytes.");
							decryptedEnd += update;
//						} while (update > 0);
					}
					catch (ShortBufferException e)
					{
						e.printStackTrace();
						System.exit(-1);
					}

//					System.out.println("Decrypted " 
//							+ "[" + (encryptedEnd - encryptedStart) + "]" + Misc.format(Arrays.copyOfRange(encryptedBuffer, encryptedStart, encryptedEnd)) 
//							+ "\nto\n" 
//							+ "[" + (decryptedEnd - debugStart) + "]" + Misc.format(Arrays.copyOfRange(decryptedBuffer, debugStart, decryptedEnd)));

					encryptedStart = encryptedEnd;
				}
				return true;
			}
			
			private boolean fillNextFrame() throws IOException
			{
				if (closed)
				{
					return false;
				}
				int extraDecrypted = decryptedEnd - frameEnd;
//				System.out.println("There were " + extraDecrypted + " extra bytes.");
				if (extraDecrypted > 0)
				{
					System.arraycopy(decryptedBuffer, frameEnd, decryptedBuffer, 0, extraDecrypted);
				}
				decryptedEnd -= frameEnd;
				frameEnd = 0;
				validStart = 0;

				// Never read more than a full encrypted.length
				encryptedStart = 0;
				encryptedEnd = 0;

				if (!readSome(META_INFO_SIZE, false))
				{
					return false;
				}

				validEnd = 0;

				for (int i = 0; i < META_INFO_SIZE; i++)
				{
					validEnd |= (decryptedBuffer[i] & 0xff) << (i * 8) ;
				}
				System.out.println("\t\tdeserialed " + validEnd);

				frameEnd = nextLarger(validEnd);

				readSome(frameEnd, true);
				validStart = META_INFO_SIZE;

				System.out.println("\t\tread "  + (readCount++) + ": " + (validEnd - validStart));
				
//				System.out.println("Valid start = " + META_INFO_SIZE + "\nValidEnd = " + validEnd);
				return true;
			}

			@Override
			public int read() throws IOException
			{
				if (closed)
				{
					return -1;
				}
				if (validStart <= validEnd)
				{
					if (!fillNextFrame())
					{
						return -1;
					}
				}
				return decryptedBuffer[validStart++] & 0xff;
			}

			@Override
			public int available() throws IOException
			{
				int available = validEnd - validStart;
				if (available != 0)
				{
					return available;
				}
				return delegate.available();
			}
			@Override
			public int read(byte[] b, int off, int len) throws IOException
			{
				if (closed)
				{
					return -1;
				}
//				System.out.println("read " + ++count);
//
//				if (count == 38)
//				{
//					System.out.println("debug me!");
//				}
				if (validStart <= validEnd)
				{
					if (!fillNextFrame())
					{
						return -1;
					}
				}
				
				int amountToRead = validEnd - validStart;
				if (amountToRead > len)
				{
					amountToRead = len;
				}
				
//				System.out.println("amount to read = " + amountToRead);
				try
				{
					System.arraycopy(decryptedBuffer, validStart, b, off, amountToRead);
				} catch (ArrayIndexOutOfBoundsException e)
				{
					System.arraycopy(decryptedBuffer, validStart, b, off, amountToRead);
				}
				validStart += amountToRead;
				
				return amountToRead;
			}

			@Override
			public void close() throws IOException
			{
				super.close();
			}
			
			@Override
			public synchronized void mark(int readlimit)
			{
				throw new RuntimeException("mark not supported!");
			}

			@Override
			public boolean markSupported()
			{
				return false;
			}

			@Override
			public long skip(long n) throws IOException
			{
				throw new RuntimeException("skip not supported!");
			}

			@Override
			public synchronized void reset() throws IOException
			{
				throw new RuntimeException("reset not supported!");
			}
		};
		return returnValue;
	}
	
	
	
	private static void nextBytesRange(Random random, byte[] bytes, int offset, int end)
	{
		for (int i = offset; i < end;)
			for (int rnd = random.nextInt(), n = Math.min(end - i, Integer.SIZE / Byte.SIZE); n-- > 0; rnd >>= Byte.SIZE)
				bytes[i++] = (byte) rnd;
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
	

	@Test
	public void simpl() throws Exception
	{
		int bufferSize = 32;
		byte[][] testStrings = new byte[][] { "".getBytes(), "short".getBytes(), "really, really, really, really, really, really, really, reall long".getBytes() };
		performTest(bufferSize, testStrings);
	}
	
	@Test
	public void testFuzzy() throws Exception
	{
		Random random = new Random(50);
		for (int i = 0; i < 1; i++)
		{
			int bufferSize = random.nextInt(1024 * 1024 / 1024);
			bufferSize = Short.MAX_VALUE / 2;
			byte[][] sequence = new byte[random.nextInt(1024)][];
			for (int j = 0; j < sequence.length; j++)
			{
				sequence[j] = new byte[random.nextInt(3 * bufferSize)];
				random.nextBytes(sequence[j]);
			}
			performTest(bufferSize, sequence);
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
}
