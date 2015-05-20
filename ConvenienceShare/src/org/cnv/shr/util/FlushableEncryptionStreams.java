package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import de.flexiprovider.core.rijndael.RijndaelKey;

public class FlushableEncryptionStreams
{
	public static int BUFFER_SIZE = 1024;
	
	// maybe we should change keys
	
	public static OutputStream createEncryptedOutputStream(OutputStream old, RijndaelKey aesKey) throws InvalidKeyException
	{
		return new FlushableEncryptionOutputStream(aesKey, old);
	}

	public static InputStream createEncryptedInputStream(InputStream old, RijndaelKey aesKey) throws InvalidKeyException
	{
		return new FlushableEncryptionInputStream(aesKey, old);
	}
	
	
	
	
	
	

	private static final class MyByteArrayOutputStream extends OutputStream
	{
		private byte[] array;
		private int index;

		MyByteArrayOutputStream(int length)
		{
			array = new byte[length];
		}

		public void flush(OutputStream output) throws IOException
		{
			if (index == 0)
			{
				return;
			}
			output.write((index >> 24) & 0xff);
			output.write((index >> 16) & 0xff);
			output.write((index >>  8) & 0xff);
			output.write((index >>  0) & 0xff);

			for (int i = 0; i < index; i++)
			{
				output.write(array[i] & 0xff);
			}
			index = 0;
		}

		@Override
		public void write(int b) throws IOException
		{
			array[index++] = (byte) (b & 0xff);
		}
		
		@Override
		public void close()
		{
			// do nothing...
		}
	}

	private static final class MyByteArrayInputStream extends InputStream
	{
		private byte[] array;
		int currentLength;
		int index;

		MyByteArrayInputStream(int length)
		{
			array = new byte[length];
		}

		public boolean fill(InputStream delegate) throws IOException
		{
			do
			{
				currentLength = 0;
				int read = delegate.read();
				if (read < 0)
				{
					return false;
				}
				currentLength |= (read            & 0xff) << 24;
				currentLength |= (delegate.read() & 0xff) << 16;
				currentLength |= (delegate.read() & 0xff) <<  8;
				currentLength |= (delegate.read() & 0xff) <<  0;
				if (currentLength > array.length)
				{
					throw new IOException("Improperly formated stream.");
				}
				for (int i = 0; i < currentLength; i++)
				{
					array[i] = (byte) delegate.read();
				}
				index = 0;
			}
			while (currentLength == 0);
			return true;
		}

		@Override
		public int read() throws IOException
		{
			if (index >= currentLength)
			{
				return -1;
			}
			return array[index++] & 0xff;
		}
		
		@Override
		public void close()
		{
			
		}
	}

	public static class FlushableEncryptionInputStream extends InputStream
	{
		private InputStream delegate;
		private CipherInputStream cipherStream;
		private MyByteArrayInputStream buffer;
		private Cipher cipher;
		private RijndaelKey aesKey;

		public FlushableEncryptionInputStream(RijndaelKey aesKey, InputStream input) throws InvalidKeyException
		{
			cipher = createCipher();
			cipher.init(Cipher.DECRYPT_MODE, aesKey);
			buffer = new MyByteArrayInputStream(cipher.getOutputSize(BUFFER_SIZE) + cipher.getBlockSize());
			this.delegate = input;
			this.aesKey = aesKey;
		}

		@Override
		public int read() throws IOException
		{
			int read;
			while (cipherStream == null || (read = cipherStream.read()) < 0)
			{
				if (cipherStream != null)
				{
					cipherStream.close();
				}
				if (!buffer.fill(delegate))
				{
					return -1;
				}

				cipher = createCipher();
				try
				{
					cipher.init(Cipher.DECRYPT_MODE, aesKey);
				}
				catch (InvalidKeyException e)
				{
					e.printStackTrace();
				}
				cipherStream = new CipherInputStream(buffer, cipher);
			}
			return read;
		}
		
		@Override
		public void close() throws IOException
		{
			if (cipherStream != null)
			{
				cipherStream.close();
			}
			delegate.close();
		}
	}

	public static class FlushableEncryptionOutputStream extends OutputStream
	{
		private MyByteArrayOutputStream buffer;
		private OutputStream delegate;
		private OutputStream cipherStream;
		private Cipher cipher;
		private int length;
		private RijndaelKey aesKey;

		public FlushableEncryptionOutputStream(RijndaelKey aesKey, OutputStream output) throws InvalidKeyException
		{
			cipher = createCipher();
			cipher.init(Cipher.ENCRYPT_MODE, aesKey);
			buffer = new MyByteArrayOutputStream(cipher.getOutputSize(BUFFER_SIZE) + cipher.getBlockSize());
			this.delegate = output;

			cipher = createCipher();
			cipher.init(Cipher.ENCRYPT_MODE, aesKey);
			cipherStream = new CipherOutputStream(buffer, cipher);
			this.aesKey = aesKey;
		}

		@Override
		public void write(int b) throws IOException
		{
			if (length >= BUFFER_SIZE)
			{
				flush();
			}
			length++;
			cipherStream.write(b);
		}

		@Override
		public void flush() throws IOException
		{
			cipherStream.close();
			buffer.flush(delegate);
			delegate.flush();

			cipher = createCipher();
			try
			{
				cipher.init(Cipher.ENCRYPT_MODE, aesKey);
			}
			catch (InvalidKeyException e)
			{
				e.printStackTrace();
			}
			cipherStream = new CipherOutputStream(buffer, cipher);
		}
		
		@Override
		public void close() throws IOException
		{
			flush();
			length = 0;
			delegate.close();
			cipherStream.close();
		}
	}

	private static final Cipher createCipher()
	{
		try
		{
			//ECB, CBC, CFB, OFB, CTR
			return Cipher.getInstance("AES128_ECB", "FlexiCore");
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e1)
		{
			e1.printStackTrace();
			return null;
		}
	}
}
