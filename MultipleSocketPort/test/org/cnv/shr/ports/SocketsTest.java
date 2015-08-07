package org.cnv.shr.ports;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Assert;
import org.junit.Test;

public class SocketsTest
{
	private boolean error = false;
	private Throwable errorReason;
	
	@Test
	public void test() throws SocketException, Exception
	{
		Random random = new Random(1776);
		
		final int port1 = 5001;
		final int port2 = 5002;
		try (MultipleSocket socket1 = new MultipleSocket(port1);
				 MultipleSocket socket2 = new MultipleSocket(port2);)
		{
			int number = 12;
			CountDownLatch latch = new CountDownLatch(2 * number);

			LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(number / 2);
			for (int i = 0; i < number; i++)
			{
				checkError();
				Runnable serve = () -> { serveThread(socket1, random.nextInt() & 0xff); latch.countDown(); };
				Runnable read  = () -> { readThread (socket2, port1);                   latch.countDown(); };
				
				queue.put(serve);
				queue.put(read);
				
				new Thread(serve).start();
				new Thread(read).start();
			}
			
			latch.await();
		}
		
		checkError();
	}

	private void checkError()
	{
		if (error)
		{
			if (errorReason != null)
			{
				errorReason.printStackTrace();
			}
			Assert.fail();
		}
	}
	
	private void error(Throwable t)
	{
		errorReason = t;
		error = true;
	}
	
	private static final int  NUMBER_OF_GIGS = 5;

	private void serveThread(MultipleSocket socket, int seed)
	{
		Random random = new Random(seed);

		try (SingleConnection accept = socket.accept();)
		{
			int length = random.nextInt(NUMBER_OF_GIGS);
			byte[] buffer = new byte[256];
			
			try (OutputStream output = accept.getOutputStream();)
			{
				output.write(seed);
				output.write(length);
				
				length *= 1024L * 1024L * 1024L;
				long offset = 0;
				while (offset < length)
				{
					random.nextBytes(buffer);
					output.write(buffer, 0, (int) Math.min(offset - length, buffer.length));
				}
			}
		}
		catch (Throwable t)
		{
			error(t);
		}
	}

	private void readThread(MultipleSocket socket, int port)
	{
		try (SingleConnection accept = socket.connect("127.0.0.1", port);)
		{
			try (InputStream input = accept.getInputStream();)
			{
				long length = input.read();
				Random random = new Random(input.read());
				
				Assert.assertEquals(length, random.nextInt(NUMBER_OF_GIGS));
				length *= 1024L * 1024L * 1024L;
				

				byte[] expected = new byte[256];
				byte[] actual   = new byte[256];
				
				long offset = 0;
				while (offset < length)
				{
					random.nextBytes(expected);
					int end = (int) Math.min(offset - length, actual.length);
					
					int numRead = 0;
					while (numRead < end)
					{
						numRead += input.read(actual, numRead, end - numRead);
					}

					for (int i = 0; i < end; i++)
					{
						Assert.assertEquals(expected[i], actual[i]);
					}
				}
				
				Assert.assertEquals(-1, input.read());
			}
		}
		catch (Throwable t)
		{
			error(t);
		}
	}
}
