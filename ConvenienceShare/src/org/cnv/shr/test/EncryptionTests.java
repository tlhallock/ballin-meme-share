package org.cnv.shr.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Security;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.dmn.KeysService;
import org.cnv.shr.util.FlushableEncryptionStreams.FlushableEncryptionInputStream;
import org.cnv.shr.util.FlushableEncryptionStreams.FlushableEncryptionOutputStream;

import de.flexiprovider.core.FlexiCoreProvider;
import de.flexiprovider.core.rijndael.RijndaelKey;

public class EncryptionTests
{


	public void testIt() throws InvalidKeyException, IOException
	{
		Security.addProvider(new FlexiCoreProvider());
		RijndaelKey aesKey = KeysService.createAesKey();

		RelayStream relay = new RelayStream();
		final FlushableEncryptionInputStream flushableEncryptionInputStream = new FlushableEncryptionInputStream(aesKey, relay.input);
		final FlushableEncryptionOutputStream flushableEncryptionOutputStream = new FlushableEncryptionOutputStream(aesKey, relay.output);

//		new Thread(new CopyStream(flushableEncryptionInputStream, System.out)).start();
		
		for (int i = 0; i < 100; i++)
		{
			int num = (int) (Math.random() * 50);
			for (int j = 0; j < num; j++)
			{
				flushableEncryptionOutputStream.write((int)(Math.random() * 50));
			}
			flushableEncryptionOutputStream.flush();
		}
		flushableEncryptionOutputStream.close();
		
		
		int read;
		while ((read = flushableEncryptionInputStream.read()) >= 0)
		{
			System.out.println(read);
		}

//		String line;
//		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//		while ((line = reader.readLine()) != null)
//		{
//			flushableEncryptionOutputStream.write(line.getBytes());
//			flushableEncryptionOutputStream.flush();
//		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	static class CopyStream implements Runnable
	{
		InputStream delegateIn;
		OutputStream delegateOut;

		public CopyStream(InputStream input, OutputStream output)
		{
			this.delegateIn = input;
			this.delegateOut = output;
		}

		@Override
		public void run()
		{
			try
			{
				int read;
				while (true)
				{
					read = delegateIn.read();
					if (read < 0)
					{
						break;
					}
					delegateOut.write(read);
				}

				delegateOut.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	static class RelayStream
	{
		private LinkedList<Integer> bytes = new LinkedList<>();
		private Lock lock = new ReentrantLock();
		private Condition condition = lock.newCondition();
		private boolean closed;

		InputStream input = new InputStream()
		{
			@Override
			public int read() throws IOException
			{
				lock.lock();
				try
				{
					while (bytes.isEmpty())
					{
						if (closed)
						{
							return -1;
						}
						try
						{
							condition.await(1, TimeUnit.SECONDS);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
					return bytes.removeLast();
				}
				finally
				{
					lock.unlock();
				}
			}
		};
		OutputStream output = new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				lock.lock();
				try
				{
					if (closed) return;
					bytes.addFirst(b & 0xff);
					condition.signalAll();
				}
				finally
				{
					lock.unlock();
				}
			}

			@Override
			public void close() throws IOException
			{
				lock.lock();
				try
				{
					closed = true;
					condition.signalAll();
				}
				finally
				{
					lock.unlock();
				}
			}
		};
	}
}
