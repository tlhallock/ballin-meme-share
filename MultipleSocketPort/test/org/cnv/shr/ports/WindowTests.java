package org.cnv.shr.ports;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.cnv.shr.ports.WindowOutputStream.Interval;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WindowTests
{
	Random random = new Random();
	
	int totalLength = 512;
	byte[] input = new byte[totalLength];
	byte[] output = new byte[totalLength];
	
	int bufferSize = 32;
	
	long currentRead = 0;
	
	WindowOutputStream outputStream = new WindowOutputStream(bufferSize);
	WindowInputStream   inputStream = new WindowInputStream (bufferSize);
	
	@Before
	public void something()
	{
		Random random = new Random();
		random.nextBytes(input);
//		random.nextBytes(output);
	}
	
	private void writeThread()
	{
		int offset = 0;
		while (offset < input.length)
		{
			int length = Math.min(1 + random.nextInt(50), input.length - offset);
			System.out.println("Writing from " + offset + " to " + (offset + length) + "=" + format(Arrays.copyOfRange(input, offset, offset + length)));
			
			outputStream.write(input, offset, length);
			offset += length;
		}
	}
	
	private void readThread()
	{
		int offset = 0;
		while (offset < output.length)
		{
			int oldOffset = offset;
			
			offset += inputStream.read(output, offset, Math.min(1 + random.nextInt(50), input.length - offset));
			currentRead = inputStream.getAmountRead();
			
			System.out.println("Read " + oldOffset + " to " + offset + "=" + format(Arrays.copyOfRange(output, oldOffset, offset)));
		}
	}

	private void transferThread() throws InterruptedException, IOException
	{
		byte[] currentBytes = new byte[5];
		while (currentRead < totalLength)
		{
			long frameStart = currentRead;
			
			System.out.println("\t\t\t\t\t\tcurrent read = " + currentRead);
			for (int i = 0; i < currentRead; i++)
			{
				Assert.assertEquals(input[i], output[i]);
			}
			
			outputStream.setRead(frameStart);
			
			Interval window = outputStream.getWindow();
			
			for (long start = window.start; start < window.stop; start += currentBytes.length)
			{
				int length = (int)(Math.min(window.stop, start + currentBytes.length) - start);
				outputStream.read(currentBytes, 0, length, start);


				for (int i = 0; i < length; i++)
				{
					if (input[(int) (start + i)] != currentBytes[i])
					{
						System.out.println("fail on " + format(Arrays.copyOfRange(currentBytes, 0, length)));
						outputStream.read(currentBytes, 0, length, start);
					}
					
					Assert.assertEquals(input[(int) (start + i)], currentBytes[i]);
				}
				
				if (Math.random() < .5)
				{
					continue;
				}
				
				System.out.println("Transferring " + currentRead + "[" + start + "-" + (start + length) + "] = " + format(Arrays.copyOfRange(currentBytes, 0, length)));
				inputStream.write(currentBytes, 0, length, start);
				
				printChunks();
			}
			
			Thread.sleep(200);
		}
	}
	
	private void printChunks()
	{
		int numChunks = 100;
		int chunkSize = input.length / 100;

		int count = 0;
		
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < numChunks; i++)
		{
			int start = i * chunkSize;
			int stop = Math.min((i + 1) * chunkSize, input.length);

			boolean done = true;
			for (int j = start; j < stop && done; j++)
			{
				done &= input[j] == output[j];
			}
			builder.append(done ? "-" : "x");
			if (done) count++;
		}
		builder.append(" " + (count / (double) numChunks) + "%");
		System.out.println(builder.toString());
		
//		System.out.println("INPUT : " + format(Arrays.copyOfRange(input,  0, 256)) + "...");
//		System.out.println("OUTPUT: " + format(Arrays.copyOfRange(output, 0, 256)) + "...");
	}
	
	@Test
	public void testCopy() throws InterruptedException, IOException
	{
		CountDownLatch latch = new CountDownLatch(2);
		
		new Thread(() -> {
			writeThread(); 
			latch.countDown();
		}).start();
		new Thread(() -> {
			readThread();  
			latch.countDown();
		}).start();
		transferThread();
		
		latch.await();
		
		Assert.assertArrayEquals(input, output);
	}
	

	public static String format(byte[] bytes)
	{
		StringBuilder builder = new StringBuilder();
		
		for (byte b : bytes)
		{
			builder.append(String.format("%02X", b & 0xff));
		}
		
		return builder.toString();
	}
}
